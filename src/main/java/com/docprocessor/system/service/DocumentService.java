package com.docprocessor.system.service;

import com.docprocessor.system.dto.DocumentResponseDTO;
import com.docprocessor.system.dto.DocumentUploadResponseDTO;
import com.docprocessor.system.exception.ResourceNotFoundException;
import com.docprocessor.system.exception.UnauthorizedException;
import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.JobType;
import com.docprocessor.system.model.User;
import com.docprocessor.system.model.ProcessingResult;
import com.docprocessor.system.repository.DocumentRepository;
import com.docprocessor.system.repository.JobRepository;
import com.docprocessor.system.repository.ProcessingResultRepository;
import com.docprocessor.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final JobService jobService;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ProcessingResultRepository processingResultRepository;

    @Value("${storage.location}")
    private String storageLocation;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    public DocumentService(DocumentRepository documentRepository,
                           JobService jobService,
                           UserRepository userRepository,
                           JobRepository jobRepository,
                           ProcessingResultRepository processingResultRepository) {
        this.documentRepository = documentRepository;
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.processingResultRepository = processingResultRepository;
    }

    @Transactional
    public DocumentUploadResponseDTO uploadDocument(MultipartFile file, Long userId) throws IOException {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Path storagePath = Paths.get(storageLocation);
        Files.createDirectories(storagePath);

        String originalFilename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path target = storagePath.resolve(uniqueFilename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Document document = new Document();
        document.setUserId(user.getId());
        document.setOriginalFilename(originalFilename);
        document.setStoredFilename(uniqueFilename);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStoragePath(target.toString());
        document.setUploadDate(LocalDateTime.now());
        document.setStatus(com.docprocessor.system.model.DocumentStatus.UPLOADED);

        Document saved = documentRepository.save(document);

        Job job = jobService.createJob(saved.getId(), JobType.TEXT_EXTRACTION);
        Long jobId = job.getId();

        DocumentUploadResponseDTO response = new DocumentUploadResponseDTO();
        response.setDocumentId(saved.getId());
        response.setJobId(jobId);
        response.setOriginalFilename(saved.getOriginalFilename());
        response.setFileSize(saved.getFileSize());
        // message has default value
        return response;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getUserDocuments(Long userId) {
        List<Document> documents = documentRepository.findByUserId(userId);
        return documents.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentById(Long id, Long userId) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!doc.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to access this document");
        }
        return mapToDto(doc);
    }

    @Transactional
    public void deleteDocument(Long id, Long userId) throws IOException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!doc.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to delete this document");
        }

        // 1. Delete associated Processing Results and Jobs
        List<Job> jobs = jobRepository.findByDocumentId(id);
        for (Job job : jobs) {
            List<ProcessingResult> results = processingResultRepository.findByJobId(job.getId());
            processingResultRepository.deleteAll(results);
        }
        jobRepository.deleteAll(jobs);

        // 2. Delete physical files from disk
        Path path = Paths.get(doc.getStoragePath());
        Files.deleteIfExists(path);

        // Delete generated thumbnail file if it exists
        if (doc.getThumbnailUrl() != null) {
            String thumbFilename = doc.getThumbnailUrl().substring(doc.getThumbnailUrl().lastIndexOf('/') + 1);
            Path thumbPath = path.getParent().resolve(thumbFilename);
            Files.deleteIfExists(thumbPath);
        }

        // 3. Delete Document record
        documentRepository.delete(doc);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of " + MAX_FILE_SIZE + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private DocumentResponseDTO mapToDto(Document doc) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(doc.getId());
        dto.setOriginalFilename(doc.getOriginalFilename());
        dto.setStoredFilename(doc.getStoredFilename());
        dto.setMimeType(doc.getMimeType());
        dto.setFileSize(doc.getFileSize());
        dto.setUploadDate(doc.getUploadDate());
        dto.setStatus(doc.getStatus());
        dto.setUserId(doc.getUserId());
        dto.setThumbnailUrl(doc.getThumbnailUrl());
        return dto;
    }
}
