package io.hansu.pacer.dto

import java.time.LocalDate

data class CoachingResponse(
    val raceGoal: RaceGoalDto?,
    val currentVdot: Double?,
    val trainingPaces: Map<String, String>?, // "5:00/km" format
    val fitnessStatus: FitnessStatus?,
    val weeklyPlan: List<DailyPlan>?,
    val recommendation: String
)

data class RaceGoalDto(
    val raceName: String,
    val raceDate: LocalDate,
    val remainingWeeks: Int,
    val targetPace: String
)

data class FitnessStatus(
    val ctl: Double, // Chronic Training Load (Fitness)
    val atl: Double, // Acute Training Load (Fatigue)
    val tsb: Double, // Training Stress Balance (Form)
    val status: String // "Productive", "Peaking", "Recovery", "Overreaching"
)

data class DailyPlan(
    val date: LocalDate,
    val dayOfWeek: String,
    val workoutType: String,
    val description: String,
    val targetDistanceKm: Double?
)
