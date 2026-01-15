package com.docprocessor.system.dto;

import java.time.LocalDateTime;

import com.docprocessor.system.model.JobStatus;
import com.docprocessor.system.model.JobType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobNotificationDTO {
    private Long jobId;
    private Long documentId;
    private Long userId;
    private JobType jobType;
    private JobStatus status;
    private String message;
    private LocalDateTime timestamp;
}
