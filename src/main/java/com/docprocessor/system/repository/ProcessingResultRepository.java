package com.docprocessor.system.repository;

import com.docprocessor.system.model.ProcessingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingResultRepository extends JpaRepository<ProcessingResult, Long> {

    List<ProcessingResult> findByJobId(Long jobId);

    Optional<ProcessingResult> findByJobIdAndResultType(Long jobId, String resultType);
}
