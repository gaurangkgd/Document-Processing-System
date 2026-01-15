package com.docprocessor.system.repository;

import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.JobStatus;
import com.docprocessor.system.model.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByDocumentId(Long documentId);

    List<Job> findByStatus(JobStatus status);

    Optional<Job> findByDocumentIdAndJobType(Long documentId, JobType jobType);

    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);

    Long countByStatus(JobStatus status);
}
