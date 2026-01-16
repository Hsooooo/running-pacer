package io.hansu.pacer.service

import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class TssCalculator {
    /**
     * TSS (Training Stress Score) 계산
     * 공식: (Duration_sec * IF^2 * 100) / 3600
     * @param durationSeconds 훈련 시간 (초)
     * @param normalizedPower (여기서는 Pace로 대체)
     * @param thresholdPower (여기서는 Threshold Pace로 대체)
     */
    fun calculateTss(durationSeconds: Int, avgPaceSecPerKm: Int, thresholdPaceSecPerKm: Int): Double {
        if (thresholdPaceSecPerKm <= 0 || avgPaceSecPerKm <= 0) return 0.0

        // IF (Intensity Factor) = Threshold Pace / Actual Pace
        // 페이스는 낮을수록 빠르므로: (Threshold Pace / Avg Pace)가 아니라 (Threshold Speed / Avg Speed) 여야 함.
        // Speed = 1 / Pace
        // IF = Avg Speed / Threshold Speed = (1/AvgPace) / (1/ThresholdPace) = ThresholdPace / AvgPace
        val intensityFactor = thresholdPaceSecPerKm.toDouble() / avgPaceSecPerKm.toDouble()

        // 1시간 기준 TSS 100점 (IF=1.0일 때)
        return (durationSeconds * intensityFactor.pow(2) * 100) / 3600
    }

    /**
     * Intensity Factor (IF) 계산
     */
    fun calculateIntensityFactor(avgPaceSecPerKm: Int, thresholdPaceSecPerKm: Int): Double {
        if (thresholdPaceSecPerKm <= 0 || avgPaceSecPerKm <= 0) return 0.0
        return thresholdPaceSecPerKm.toDouble() / avgPaceSecPerKm.toDouble()
    }
}
