package io.hansu.pacer.service

import org.springframework.stereotype.Service
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

@Service
class VdotCalculator {

    // Jack Daniels' Running Formula Approximation
    fun estimateVdot(distanceMeters: Int, timeSeconds: Int): Double {
        if (timeSeconds <= 0) return 0.0
        val velocity = distanceMeters.toDouble() / (timeSeconds.toDouble() / 60.0) // meters per minute
        
        // VO2 cost formula (Daniels & Gilbert)
        val percentMax = 0.8 + 0.1894393 * exp(-0.012778 * timeSeconds / 60.0) + 
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
