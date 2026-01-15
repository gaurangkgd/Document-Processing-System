package com.docprocessor.system.repository;

import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserId(Long userId);

    List<Document> findByUserIdAndStatus(Long userId, DocumentStatus status);

    Optional<Document> findByStoredFilename(String storedFilename);

    Long countByUserId(Long userId);
}
