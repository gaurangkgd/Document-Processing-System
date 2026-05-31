package com.docprocessor.system.service;

import com.docprocessor.system.dto.SearchResultDTO;
import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.ProcessingResult;
import com.docprocessor.system.repository.DocumentRepository;
import com.docprocessor.system.repository.JobRepository;
import com.docprocessor.system.repository.ProcessingResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final ProcessingResultRepository processingResultRepository;
    private final JobRepository jobRepository;
    private final DocumentRepository documentRepository;

    public SearchService(ProcessingResultRepository processingResultRepository,
                         JobRepository jobRepository,
                         DocumentRepository documentRepository) {
        this.processingResultRepository = processingResultRepository;
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResultDTO> search(String keyword) {
        log.info("=== SEARCH START ===");
        log.info("Searching for keyword: '{}'", keyword);

        List<ProcessingResult> results = processingResultRepository.searchInExtractedText(keyword);
        log.info("Database query returned {} processing results", results.size());

        if (results.isEmpty()) {
            log.warn("No results from database query for keyword: '{}'", keyword);
        }

        List<SearchResultDTO> searchResults = new ArrayList<>();

        for (ProcessingResult pr : results) {
            log.info("Processing result ID: {}, JobId: {}", pr.getId(), pr.getJobId());
            try {
                // Fetch Job by jobId
                Optional<Job> jobOpt = jobRepository.findById(pr.getJobId());
                if (jobOpt.isEmpty()) {
                    log.warn("Job not found for processing result {}", pr.getId());
                    continue;
                }
                Job job = jobOpt.get();
                log.info("Found job: {}, documentId: {}", job.getId(), job.getDocumentId());

                // Fetch Document by documentId
                Optional<Document> docOpt = documentRepository.findById(job.getDocumentId());
                if (docOpt.isEmpty()) {
                    log.warn("Document not found for job {}", job.getId());
                    continue;
                }
                Document document = docOpt.get();
                log.info("Found document: {}, name: {}", document.getId(), document.getOriginalFilename());

                SearchResultDTO dto = new SearchResultDTO();
                dto.setId(pr.getId());
                dto.setJobId(job.getId());
                dto.setDocumentId(document.getId());
                dto.setDocumentName(document.getOriginalFilename());
                dto.setExtractedText(getSnippet(pr.getResultData(), keyword));
                dto.setUploadedAt(document.getUploadDate());

                searchResults.add(dto);
                log.info("Added search result for document: {}", document.getOriginalFilename());
            } catch (Exception e) {
                log.error("Error processing search result {}: {}", pr.getId(), e.getMessage(), e);
            }
        }

        log.info("=== SEARCH END === Returning {} search results", searchResults.size());
        return searchResults;
    }

    private String getSnippet(String fullText, String keyword) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        int index = fullText.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) return fullText.substring(0, Math.min(200, fullText.length()));

        int start = Math.max(0, index - 100);
        int end = Math.min(fullText.length(), index + keyword.length() + 100);

        return "..." + fullText.substring(start, end) + "...";
    }
}