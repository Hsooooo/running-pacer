package io.hansu.pacer.domain.job.repository

import io.hansu.pacer.domain.job.IngestJobEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface IngestJobRepository : JpaRepository<IngestJobEntity, Long> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT * FROM ingest_jobs
            WHERE status = 'PENDING' AND next_run_at <= :now
            ORDER BY next_run_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """
    )
    fun claimPendingJobs(@Param("now") now: LocalDateTime, @Param("limit") limit: Int = 10): List<IngestJobEntity>

    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            UPDATE ingest_jobs
            SET status = 'RUNNING', retry_count = retry_count + 1, updated_at = CURRENT_TIMESTAMP
            WHERE job_id = :jobId
        """
    )
    fun markAsRunning(@Param("jobId") jobId: Long)

    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            UPDATE ingest_jobs
            SET status = 'DONE', updated_at = CURRENT_TIMESTAMP
            WHERE job_id = :jobId
        """
    )
    fun markAsDone(@Param("jobId") jobId: Long)

    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            UPDATE ingest_jobs
            SET status = 'FAILED', last_error = :error, next_run_at = :nextRunAt, updated_at = CURRENT_TIMESTAMP
            WHERE job_id = :jobId
        """
    )
    fun markAsFailedWithRetry(@Param("jobId") jobId: Long, @Param("error") error: String, @Param("nextRunAt") nextRunAt: LocalDateTime)

    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            UPDATE ingest_jobs
            SET status = 'FAILED', last_error = :error, updated_at = CURRENT_TIMESTAMP
            WHERE job_id = :jobId
        """
    )
    fun markAsFailedPermanently(@Param("jobId") jobId: Long, @Param("error") error: String)
}
