package com.docprocessor.system.dto;

import com.docprocessor.system.model.JobType;
import com.docprocessor.system.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponseDTO {
    private Long jobId;
    private Long documentId;
    private JobType jobType;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer retryCount;
    private String errorMessage;
}
