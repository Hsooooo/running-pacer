package io.hansu.pacer.service

import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.pow
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

/** 거리별 Best Effort (개인 최고 기록) 정보 */
data class BestEffort(
        val distanceType: DistanceType,
        val activityId: Long,
        val distanceM: Int,
        val timeSeconds: Int,
        val date: LocalDate,
        val vdot: Double
)

enum class DistanceType(val minM: Int, val maxM: Int, val displayName: String) {
    FIVE_K(4800, 5200, "5K"), // 5km ± 200m
    TEN_K(9500, 10500, "10K"), // 10km ± 500m
    HALF(20500, 21600, "Half"), // 21.1km ± 500m
    FULL(41500, 43000, "Full") // 42.2km ± 700m
}

@Service
class VdotCalculator(private val jdbc: NamedParameterJdbcTemplate) {

    /**
     * 사용자의 Best Effort들을 조회하여 가장 높은 VDOT 산출
     * @param userId 사용자 ID
     * @param since 해당 날짜 이후의 기록만 고려 (기본 1년)
     */
    fun getCurrentVdot(userId: Long, since: LocalDate = LocalDate.now().minusYears(1)): Double {
        val bestEfforts = getBestEfforts(userId, since)
        return if (bestEfforts.isNotEmpty()) {
            bestEfforts.maxOf { it.vdot }
        } else {
            // Best Effort가 없을 경우 최근 평균 페이스로 추정
            estimateVdotFromRecentRuns(userId, since) ?: 30.0 // 기본값
        }
    }

    /** 거리별 Best Effort 목록 조회 */
    fun getBestEfforts(
            userId: Long,
            since: LocalDate = LocalDate.now().minusYears(1)
    ): List<BestEffort> {
        return DistanceType.entries.mapNotNull { distanceType ->
            findBestEffortForDistance(userId, distanceType, since)
        }
    }

    /** 특정 거리 범위에서 가장 빠른 기록 찾기 */
    private fun findBestEffortForDistance(
            userId: Long,
            distanceType: DistanceType,
            since: LocalDate
    ): BestEffort? {
        val sql =
                """
            SELECT activity_id, distance_m, moving_time_s, start_time_utc::date as run_date
            FROM activities
            WHERE user_id = :userId
              AND sport_type = 'RUN'
              AND distance_m BETWEEN :minM AND :maxM
              AND start_time_utc >= :since
              AND moving_time_s > 0
            ORDER BY moving_time_s ASC
            LIMIT 1
        """.trimIndent()

        val params =
                mapOf(
                        "userId" to userId,
                        "minM" to distanceType.minM,
                        "maxM" to distanceType.maxM,
                        "since" to since.atStartOfDay()
                )

        return try {
            jdbc.queryForObject(sql, params) { rs, _ ->
                val distanceM = rs.getInt("distance_m")
                val timeSeconds = rs.getInt("moving_time_s")
                BestEffort(
                        distanceType = distanceType,
                        activityId = rs.getLong("activity_id"),
                        distanceM = distanceM,
                        timeSeconds = timeSeconds,
                        date = rs.getDate("run_date").toLocalDate(),
                        vdot = estimateVdot(distanceM, timeSeconds)
                )
            }
        } catch (e: org.springframework.dao.EmptyResultDataAccessException) {
            null
        }
    }

    /** 최근 러닝 평균 페이스로 VDOT 추정 (Best Effort가 없을 때 fallback) */
    private fun estimateVdotFromRecentRuns(userId: Long, since: LocalDate): Double? {
        val sql =
                """
            SELECT AVG(avg_pace_sec_per_km) as avg_pace
            FROM activities
            WHERE user_id = :userId
              AND sport_type = 'RUN'
              AND start_time_utc >= :since
              AND avg_pace_sec_per_km IS NOT NULL
              AND avg_pace_sec_per_km > 0
        """.trimIndent()

        val params = mapOf("userId" to userId, "since" to since.atStartOfDay())

        return try {
            val avgPace = jdbc.queryForObject(sql, params, Double::class.java) ?: return null
            if (avgPace <= 0) return null

            // Easy pace가 Threshold의 약 1.25배라고 가정
            val estimatedThresholdPace = avgPace / 1.25
            // VDOT 50 = Threshold 240s/km 기준
            50.0 * (240.0 / estimatedThresholdPace)
        } catch (e: Exception) {
            null
        }
    }

    // Jack Daniels' Running Formula Approximation
    fun estimateVdot(distanceMeters: Int, timeSeconds: Int): Double {
        if (timeSeconds <= 0) return 0.0
        val velocity =
                distanceMeters.toDouble() / (timeSeconds.toDouble() / 60.0) // meters per minute

        // VO2 cost formula (Daniels & Gilbert)
        val percentMax =
                0.8 +
                        0.1894393 * exp(-0.012778 * timeSeconds / 60.0) +
                        0.2989558 * exp(-0.1932605 * timeSeconds / 60.0)

        val vo2 = -4.60 + 0.182258 * velocity + 0.000104 * velocity.pow(2)

        return vo2 / percentMax
    }

    fun calculateTrainingPaces(vdot: Double): Map<String, Int> {
        // Simple approximation for pace zones (seconds per km)
        // Based on VDOT tables logic (simplified)

        // E-Pace: ~65-79% VO2max -> ~0.70 intensity
        // M-Pace: ~80-90% -> ~0.82 intensity
        // T-Pace: ~88-92% -> ~0.88 intensity
        // I-Pace: ~98-100% -> ~0.97 intensity
        // R-Pace: ~105-120% -> ~1.10 intensity

        // Base reference: 5K pace from VDOT
        // This is a rough heuristic, ideally use lookup tables

        // 5k time estimate from VDOT
        // (Roughly: VDOT 30 = 30:40, VDOT 40 = 22:41, VDOT 50 = 19:57, VDOT 60 = 17:03)
        // Formula is hard to reverse, using simple multipliers on VDOT 50 baseline (4:00/km)

        val refPace50 = 240 // 4:00/km for VDOT 50
        val factor = 50.0 / vdot

        // Adjust factor non-linearly (faster runners are more efficient)
        val adjustedFactor = factor.pow(0.95)

        val basePace = (refPace50 * adjustedFactor).toInt() // Approx Threshold pace

        return mapOf(
                "EASY" to (basePace * 1.25).toInt(),
                "MARATHON" to (basePace * 1.10).toInt(),
                "THRESHOLD" to basePace,
                "INTERVAL" to (basePace * 0.92).toInt(),
                "REPETITION" to (basePace * 0.88).toInt()
        )
    }
}
