package com.docprocessor.system.controller;

import com.docprocessor.system.dto.SearchResultDTO;
import com.docprocessor.system.model.ProcessingResult;
import com.docprocessor.system.repository.ProcessingResultRepository;
import com.docprocessor.system.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final ProcessingResultRepository processingResultRepository;

    public SearchController(SearchService searchService, ProcessingResultRepository processingResultRepository) {
        this.searchService = searchService;
        this.processingResultRepository = processingResultRepository;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultDTO>> search(@RequestParam String query) {
        List<SearchResultDTO> results = searchService.search(query);
        return ResponseEntity.ok(results);
    }

    // Debug endpoint to check database content
    @GetMapping("/debug")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> debug() {
        List<ProcessingResult> allResults = processingResultRepository.findAll();
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("totalProcessingResults", allResults.size());

        List<Map<String, Object>> resultDetails = allResults.stream()
            .filter(pr -> "EXTRACTED_TEXT".equals(pr.getResultType()))
            .map(pr -> {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", pr.getId());
                detail.put("jobId", pr.getJobId());
                detail.put("resultType", pr.getResultType());
                detail.put("textLength", pr.getResultData() != null ? pr.getResultData().length() : 0);
                detail.put("textPreview", pr.getResultData() != null && pr.getResultData().length() > 100
                    ? pr.getResultData().substring(0, 100) + "..."
                    : pr.getResultData());
                return detail;
            })
            .toList();

        debugInfo.put("extractedTextResults", resultDetails);
        return ResponseEntity.ok(debugInfo);
    }

    // Test search without auth - for debugging
    @GetMapping("/test")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> testSearch(@RequestParam String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("searchQuery", query);

        // Method 1: SQL query
        List<ProcessingResult> sqlResults = processingResultRepository.searchInExtractedText(query);
        result.put("sqlQueryResultCount", sqlResults.size());

        // Method 2: Java filtering (bypass SQL)
        List<ProcessingResult> allExtracted = processingResultRepository.findByResultType("EXTRACTED_TEXT");
        result.put("totalExtractedTextRecords", allExtracted.size());

        List<ProcessingResult> javaFiltered = allExtracted.stream()
            .filter(pr -> pr.getResultData() != null &&
                         pr.getResultData().toLowerCase().contains(query.toLowerCase()))
            .toList();
        result.put("javaFilteredResultCount", javaFiltered.size());

        List<Map<String, Object>> details = new ArrayList<>();
        for (ProcessingResult pr : javaFiltered) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", pr.getId());
            detail.put("jobId", pr.getJobId());
            detail.put("textPreview", pr.getResultData() != null && pr.getResultData().length() > 50
                ? pr.getResultData().substring(0, 50) + "..."
                : pr.getResultData());
            details.add(detail);
        }
        result.put("results", details);

        return ResponseEntity.ok(result);
    }
}
