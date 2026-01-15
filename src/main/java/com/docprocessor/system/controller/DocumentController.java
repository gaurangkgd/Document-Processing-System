package com.docprocessor.system.controller;

import com.docprocessor.system.dto.DocumentResponseDTO;
import com.docprocessor.system.dto.DocumentUploadResponseDTO;
import com.docprocessor.system.exception.ResourceNotFoundException;
import com.docprocessor.system.exception.UnauthorizedException;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.UserRepository;
import com.docprocessor.system.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponseDTO> uploadDocument(@RequestParam("file") MultipartFile file,
                                                                    Authentication authentication) throws IOException {
        Long userId = extractUserId(authentication);
        DocumentUploadResponseDTO response = documentService.uploadDocument(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getUserDocuments(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<DocumentResponseDTO> docs = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id, Authentication authentication) {
        Long userId = extractUserId(authentication);
        DocumentResponseDTO dto = documentService.getDocumentById(id, userId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication authentication) throws IOException {
        Long userId = extractUserId(authentication);
        documentService.deleteDocument(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Helper to extract userId from Authentication
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
            // fallback
            username = String.valueOf(principal);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return user.getId();
    }
}
