package com.docprocessor.system.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.docprocessor.system.dto.JobNotificationDTO;
import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.repository.DocumentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final DocumentRepository documentRepository;

    public NotificationService(SimpMessagingTemplate simpMessagingTemplate,
                               DocumentRepository documentRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.documentRepository = documentRepository;
    }

    public void sendJobNotification(Job job, String message) {
        if (job == null) {
            log.warn("sendJobNotification called with null job");
            return;
        }

        Long userId = null;
        try {
            if (job.getDocument() != null && job.getDocument().getUserId() != null) {
                userId = job.getDocument().getUserId();
            } else if (job.getDocumentId() != null) {
                Optional<Document> doc = documentRepository.findById(job.getDocumentId());
                if (doc.isPresent()) {
                    userId = doc.get().getUserId();
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve userId for job {}: {}", job.getId(), ex.getMessage());
        }

        if (userId == null) {
            log.warn("Cannot send notification for job {} because userId could not be determined", job.getId());
            return;
        }

        JobNotificationDTO dto = new JobNotificationDTO(
                job.getId(),
                job.getDocumentId(),
                userId,
                job.getJobType(),
                job.getStatus(),
                message,
                LocalDateTime.now()
        );

        String destination = "/topic/job-updates/" + userId;
        try {
            simpMessagingTemplate.convertAndSend(destination, dto);
            log.debug("Sent job notification to {} for job {}", destination, job.getId());
        } catch (Exception ex) {
            log.error("Failed to send job notification for job {} to {}: {}", job.getId(), destination, ex.getMessage(), ex);
        }
    }

    public void sendJobCompletedNotification(Job job) {
        sendJobNotification(job, "Job completed successfully");
    }

    public void sendJobFailedNotification(Job job, String errorMessage) {
        sendJobNotification(job, "Job failed: " + (errorMessage == null ? "Unknown error" : errorMessage));
    }
}
