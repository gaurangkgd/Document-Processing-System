package com.docprocessor.system.dto;

import com.docprocessor.system.model.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponseDTO {
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadDate;
    private DocumentStatus status;
    private Long userId;
    private String thumbnailUrl;
}
