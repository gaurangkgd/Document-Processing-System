package com.docprocessor.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchResultDTO {
    private Long id;
    private Long jobId;
    private Long documentId;
    private String documentName;
    private String extractedText;
    private LocalDateTime uploadedAt;
}