package com.docprocessor.system.repository;

import com.docprocessor.system.model.ProcessingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingResultRepository extends JpaRepository<ProcessingResult, Long> {

    List<ProcessingResult> findByJobId(Long jobId);

    Optional<ProcessingResult> findByJobIdAndResultType(Long jobId, String resultType);

    // Find all extracted text results
    List<ProcessingResult> findByResultType(String resultType);

    @Query(value = """
        SELECT * FROM processing_results 
        WHERE result_type = 'EXTRACTED_TEXT' 
        AND result_data ILIKE CONCAT('%', :searchTerm, '%')
        """, nativeQuery = true)
    List<ProcessingResult> searchInExtractedText(@Param("searchTerm") String searchTerm);
}
