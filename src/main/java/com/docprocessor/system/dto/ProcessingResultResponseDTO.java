package com.docprocessor.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessingResultResponseDTO {
    private Long id;
    private Long jobId;
    private String resultType;
    private String resultData;
    private LocalDateTime createdAt;
}

