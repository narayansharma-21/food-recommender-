package com.narayansharma.foodrecommender.platform.jobs;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select job from BackgroundJob job
			where (job.status = 'PENDING' and job.availableAt <= :now)
			   or (job.status = 'RUNNING' and job.lockedAt <= :staleBefore)
			order by job.createdAt
			""")
	List<BackgroundJob> findReady(Instant now, Instant staleBefore, Pageable pageable);
}
