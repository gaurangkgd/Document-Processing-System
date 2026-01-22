package com.docprocessor.system.service;

import com.docprocessor.system.dto.SearchResultDTO;
import com.docprocessor.system.model.ProcessingResult;
import com.docprocessor.system.repository.ProcessingResultRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final ProcessingResultRepository processingResultRepository;

    public SearchService(ProcessingResultRepository processingResultRepository) {
        this.processingResultRepository = processingResultRepository;
    }

    public List<SearchResultDTO> search(String keyword) {
        List<ProcessingResult> results = processingResultRepository.searchInExtractedText(keyword);

        return results.stream()
                .map(pr -> {
                    SearchResultDTO dto = new SearchResultDTO();
                    dto.setJobId(pr.getJobId());
                    dto.setExtractedText(getSnippet(pr.getResultData(), keyword));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String getSnippet(String fullText, String keyword) {
        int index = fullText.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) return fullText.substring(0, Math.min(200, fullText.length()));

        int start = Math.max(0, index - 100);
        int end = Math.min(fullText.length(), index + keyword.length() + 100);

        return "..." + fullText.substring(start, end) + "...";
    }
}