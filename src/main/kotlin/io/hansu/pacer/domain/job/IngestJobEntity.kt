package io.hansu.pacer.domain.job

import jakarta.persistence.*

@Entity
@Table(
    name = "ingest_jobs",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_provider_job", columnNames = ["provider", "provider_ref_id", "job_type"])
    ],
    indexes = [Index(name = "idx_jobs_status_next", columnList = "status, next_run_at")]
)
class IngestJobEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    val id: Long = 0,

    @Column(name = "provider", nullable = false, length = 20)
    val provider: String,

    @Column(name = "provider_ref_id", nullable = false)
    val providerRefId: Long,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "job_type", nullable = false, length = 30)
    val jobType: String,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDING",

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "next_run_at", nullable = false)
    var nextRunAt: java.time.LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_DONE = "DONE"
        const val STATUS_FAILED = "FAILED"

        const val PROVIDER_STRAVA = "STRAVA"
        const val JOB_TYPE_ACTIVITY_UPSERT = "ACTIVITY_UPSERT"

        const val MAX_RETRY_COUNT = 5
    }
}
