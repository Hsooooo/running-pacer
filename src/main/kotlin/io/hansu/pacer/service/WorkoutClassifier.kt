package io.hansu.pacer.service

import io.hansu.pacer.domain.training.WorkoutType
import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.stereotype.Service

/**
 * 활동의 워크아웃 타입을 자동 분류하는 서비스
 *
 * 분류 기준 (Threshold Pace 기준):
 * - RECOVERY: 페이스 >= Threshold × 1.30 (매우 느림)
 * - EASY: 페이스 >= Threshold × 1.15 (느림)
 * - TEMPO: Threshold ± 7% 구간
 * - INTERVAL: 페이스 <= Threshold × 0.93 (빠름) + 거리 < 12km
 * - LONG_RUN: 거리 >= 15km 이면서 Easy 페이스대
 */
@Service
class WorkoutClassifier(
        private val vdotCalculator: VdotCalculator,
        private val tssCalculator: TssCalculator
) {

    /**
     * 활동을 분석하여 워크아웃 분류 결과 반환
     * @param userId 사용자 ID
     * @param distanceM 거리 (미터)
     * @param movingTimeS 이동 시간 (초)
     * @param avgPaceSecPerKm 평균 페이스 (초/km), null이면 계산
     */
    fun classifyWorkout(
            userId: Long,
            distanceM: Int,
            movingTimeS: Int,
            avgPaceSecPerKm: Int?
    ): WorkoutClassification {
        // 1. 현재 VDOT 조회
        val currentVdot = vdotCalculator.getCurrentVdot(userId)

        // 2. Threshold Pace 계산
        val trainingPaces = vdotCalculator.calculateTrainingPaces(currentVdot)
        val thresholdPace = trainingPaces["THRESHOLD"] ?: 300 // 기본 5:00/km

        // 3. 실제 페이스 계산
        val actualPace =
                avgPaceSecPerKm
                        ?: run {
                            if (distanceM > 0 && movingTimeS > 0) {
                                (movingTimeS.toDouble() / (distanceM / 1000.0)).toInt()
                            } else {
                                thresholdPace // fallback
                            }
                        }

        // 4. 워크아웃 타입 분류
        val workoutType = classifyByPaceAndDistance(actualPace, thresholdPace, distanceM)

        // 5. TSS 및 IF 계산
        val intensityFactor = tssCalculator.calculateIntensityFactor(actualPace, thresholdPace)
        val tss = tssCalculator.calculateTss(movingTimeS, actualPace, thresholdPace)

        // 6. 이 러닝에서의 VDOT 추정 (5K 이상 러닝에서만)
        val runVdot =
                if (distanceM >= 4800) {
                    vdotCalculator.estimateVdot(distanceM, movingTimeS)
                } else {
                    null
                }

        return WorkoutClassification(
                workoutType = workoutType,
                tss = BigDecimal(tss).setScale(2, RoundingMode.HALF_UP),
                intensityFactor = BigDecimal(intensityFactor).setScale(3, RoundingMode.HALF_UP),
                vdotEstimate = runVdot?.let { BigDecimal(it).setScale(1, RoundingMode.HALF_UP) },
                thresholdPaceUsed = thresholdPace,
                currentVdot = currentVdot
        )
    }

    /** 페이스와 거리 기반으로 워크아웃 타입 분류 */
    private fun classifyByPaceAndDistance(
            actualPace: Int,
            thresholdPace: Int,
            distanceM: Int
    ): WorkoutType {
        val paceRatio = actualPace.toDouble() / thresholdPace.toDouble()
        val distanceKm = distanceM / 1000.0

        return when {
            // Long Run: 15km 이상이면서 Easy 페이스 (Threshold × 1.15 이상)
            distanceKm >= 15.0 && paceRatio >= 1.10 -> WorkoutType.LONG_RUN

            // Recovery: 매우 느린 페이스 (Threshold × 1.30 이상)
            paceRatio >= 1.30 -> WorkoutType.RECOVERY

            // Easy: 느린 페이스 (Threshold × 1.15 이상)
            paceRatio >= 1.15 -> WorkoutType.EASY

            // Interval: 빠른 페이스 (Threshold × 0.93 이하) + 짧은 거리
            paceRatio <= 0.93 && distanceKm < 12.0 -> WorkoutType.INTERVAL

            // Tempo: Threshold 근처 (± 15%)
            paceRatio in 0.85..1.15 -> WorkoutType.TEMPO

            // 그 외는 Easy로 기본 분류
            else -> WorkoutType.EASY
        }
    }
}

/** 워크아웃 분류 결과 */
data class WorkoutClassification(
        val workoutType: WorkoutType,
        val tss: BigDecimal,
        val intensityFactor: BigDecimal,
        val vdotEstimate: BigDecimal?,
        val thresholdPaceUsed: Int,
        val currentVdot: Double
)
