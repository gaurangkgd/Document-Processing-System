package com.docprocessor.system.dto;

import lombok.Data;

@Data
public class DocumentUploadResponseDTO {
    private Long documentId;
    private Long jobId;
    private String originalFilename;
    private Long fileSize;
    private String message = "File uploaded successfully. Processing started.";
}
