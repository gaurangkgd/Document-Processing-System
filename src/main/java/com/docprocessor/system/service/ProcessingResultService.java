package com.docprocessor.system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.docprocessor.system.repository.ProcessingResultRepository;
import com.docprocessor.system.model.ProcessingResult;
import com.docprocessor.system.dto.ProcessingResultResponseDTO;

@Service
public class ProcessingResultService {

    private final ProcessingResultRepository processingResultRepository;

    public ProcessingResultService(ProcessingResultRepository processingResultRepository) {
        this.processingResultRepository = processingResultRepository;
    }

    public ProcessingResult saveResult(Long jobId, String resultType, String resultData) {
        ProcessingResult result = new ProcessingResult();
        // set fields - adapt names if your entity uses different field names or relationships
        result.setJobId(jobId);
        result.setResultType(resultType);
        result.setResultData(resultData);
        result.setCreatedAt(LocalDateTime.now());
        return processingResultRepository.save(result);
    }

    public List<ProcessingResultResponseDTO> getResultsByJobId(Long jobId) {
        List<ProcessingResult> results = processingResultRepository.findByJobId(jobId);
        return results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProcessingResultResponseDTO getResultByJobIdAndType(Long jobId, String resultType) {
        ProcessingResult result = processingResultRepository
                .findByJobIdAndResultType(jobId, resultType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Processing result for jobId=%d and type='%s' not found", jobId, resultType)
                ));
        return mapToDto(result);
    }

    private ProcessingResultResponseDTO mapToDto(ProcessingResult result) {
        ProcessingResultResponseDTO dto = new ProcessingResultResponseDTO();
        dto.setId(result.getId());
        dto.setJobId(result.getJobId());
        dto.setResultType(result.getResultType());
        dto.setResultData(result.getResultData());
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }
}
