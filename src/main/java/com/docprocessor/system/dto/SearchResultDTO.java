package com.docprocessor.system.dto;

import lombok.Data;

@Data
public class SearchResultDTO {
    private Long jobId;
    private String extractedText;
}