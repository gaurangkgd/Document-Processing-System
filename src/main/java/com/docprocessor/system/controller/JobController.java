package com.docprocessor.system.controller;

import com.docprocessor.system.dto.JobStatusResponseDTO;
import com.docprocessor.system.exception.ResourceNotFoundException;
import com.docprocessor.system.exception.UnauthorizedException;
import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.DocumentRepository;
import com.docprocessor.system.repository.UserRepository;
import com.docprocessor.system.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job status and management endpoints")
public class JobController {

    private final JobService jobService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<JobStatusResponseDTO>> getAllUserJobs(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<JobStatusResponseDTO> jobs = jobService.getUserJobs(userId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable Long id, Authentication authentication) {
        JobStatusResponseDTO dto = jobService.getJobById(id);
        Long documentId = dto.getDocumentId();
        Long userId = extractUserId(authentication);
        verifyDocumentOwnership(documentId, userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<JobStatusResponseDTO>> getJobsByDocument(@PathVariable Long documentId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        verifyDocumentOwnership(documentId, userId);
        List<JobStatusResponseDTO> jobs = jobService.getJobsByDocumentId(documentId);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Job> retryJob(@PathVariable Long id, Authentication authentication) {
        // Security: ensure the job's document belongs to the authenticated user
        JobStatusResponseDTO dto = jobService.getJobById(id);
        Long documentId = dto.getDocumentId();
        Long userId = extractUserId(authentication);
        verifyDocumentOwnership(documentId, userId);

        Job retried = jobService.retryFailedJob(id);
        return ResponseEntity.ok(retried);
    }

    // Helper to extract userId from Authentication (uses username lookup)
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            username = String.valueOf(principal);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return user.getId();
    }

    private void verifyDocumentOwnership(Long documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        if (!doc.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to access this resource");
        }
    }
}
