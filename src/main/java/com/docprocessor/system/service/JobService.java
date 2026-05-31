package com.docprocessor.system.service;

import com.docprocessor.system.dto.JobStatusResponseDTO;
import com.docprocessor.system.exception.InvalidJobOperationException;
import com.docprocessor.system.exception.ResourceNotFoundException;
import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.JobStatus;
import com.docprocessor.system.model.JobType;
import com.docprocessor.system.repository.DocumentRepository;
import com.docprocessor.system.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DocumentRepository documentRepository;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

//    @Transactional
//    public Job createJob(Document document , Long documentId, JobType jobType) {
//        // Create Job entity with default type
//        Job job = new Job();
//        job.setDocumentId(documentId);  // Set the ID directly!
//        job.setJobType(JobType.DOCUMENT_PROCESSING);
//        job.setStatus(JobStatus.QUEUED);
//        job.setRetryCount(0);
//        job.setMaxRetries(3);
//        job.setCreatedAt(LocalDateTime.now());
//
//        // Save to database
//        Job savedJob = jobRepository.save(job);
//
//        // Send job message to RabbitMQ queue
////        sendJobToQueue(savedJob);
//
//        return savedJob;
//    }

    @Transactional
    public Job createJob(Long documentId, JobType jobType) {
        // Find document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        // Create Job entity with status QUEUED
        Job job = new Job();
        job.setDocument(document);
        job.setDocumentId(documentId);  // ← Now this matches the parameter!    `
        job.setJobType(jobType);
        job.setStatus(JobStatus.QUEUED);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setCreatedAt(LocalDateTime.now());

        // Save to database
        Job savedJob = jobRepository.save(job);

        // Send job message to RabbitMQ queue
        sendJobToQueue(savedJob);
        rabbitTemplate.convertAndSend("document-processing-queue", savedJob.getId().toString());

        return savedJob;
    }

    public JobStatusResponseDTO getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        return mapToJobStatusResponseDTO(job);
    }

    public List<JobStatusResponseDTO> getJobsByDocumentId(Long documentId) {
        List<Job> jobs = jobRepository.findByDocumentId(documentId);
        return jobs.stream()
                .map(this::mapToJobStatusResponseDTO)
                .collect(Collectors.toList());
    }

    public List<JobStatusResponseDTO> getUserJobs(Long userId) {
        // Get all documents for the user
        List<Document> userDocuments = documentRepository.findByUserId(userId);
        List<Long> documentIds = userDocuments.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // Get all jobs for those documents
        List<Job> jobs = jobRepository.findAll().stream()
                .filter(job -> documentIds.contains(job.getDocumentId()))
                .collect(Collectors.toList());

        return jobs.stream()
                .map(this::mapToJobStatusResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateJobStatus(Long jobId, JobStatus newStatus) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        job.setStatus(newStatus);

        // Update timestamps based on status
        switch (newStatus) {
            case PROCESSING:
                job.setStartedAt(LocalDateTime.now());
                break;
            case COMPLETED:
            case FAILED:
                if (job.getStartedAt() == null) {
                    job.setStartedAt(LocalDateTime.now());
                }
                job.setCompletedAt(LocalDateTime.now());
                break;
        }

        jobRepository.save(job);
    }

    @Transactional
    public Job retryFailedJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Check if job status is FAILED
        if (job.getStatus() != JobStatus.FAILED) {
            throw new InvalidJobOperationException("Only failed jobs can be retried");
        }

        // Increment retry count
        job.setRetryCount(job.getRetryCount() + 1);

        // Reset status to QUEUED
        job.setStatus(JobStatus.QUEUED);
        job.setStartedAt(null);
        job.setCompletedAt(null);

        // Save updated job
        Job savedJob = jobRepository.save(job);

        // Send to queue again
        sendJobToQueue(savedJob);

        return savedJob;
    }

    // Helper method for sending job to RabbitMQ queue
    private void sendJobToQueue(Job job) {
        try {
            rabbitTemplate.convertAndSend(queueName, job.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send job to queue", e);
        }
    }

    // DTO mapping helper method
    private JobStatusResponseDTO mapToJobStatusResponseDTO(Job job) {
        return JobStatusResponseDTO.builder()
                .jobId(job.getId())
                .documentId(job.getDocumentId())  // Use direct field, not lazy-loaded entity
                .jobType(job.getJobType())
                .status(job.getStatus())
                .retryCount(job.getRetryCount())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
