package io.hansu.pacer.service

import io.hansu.pacer.util.HourlyWeather
import io.hansu.pacer.util.KmaWeatherClient
import io.hansu.pacer.util.WeatherData
import kotlin.math.max
import kotlin.math.min
import org.springframework.stereotype.Service

/**
 * 시간대별 달리기 지수 계산 서비스
 *
 * 계산 요소 및 가중치:
 * - 기온 (30%): 10-15°C가 최적, 5°C 차이당 -10점
 * - 습도 (20%): 40-60%가 최적, 10% 차이당 -5점
 * - 풍속 (20%): 0-3m/s가 최적, 1m/s당 -5점
 * - 강수 (20%): 비 -40점, 눈 -50점
 * - 하늘 (10%): 맑음 최적, 흐림 -10점
 */
@Service
class RunningIndexService(private val weatherClient: KmaWeatherClient) {

    companion object {
        // 최적 기온 범위
        const val OPTIMAL_TEMP_MIN = 10
        const val OPTIMAL_TEMP_MAX = 15

        // 최적 습도 범위
        const val OPTIMAL_HUMIDITY_MIN = 40
        const val OPTIMAL_HUMIDITY_MAX = 60

        // 최적 풍속 (m/s x 10)
        const val OPTIMAL_WIND_MAX = 30 // 3.0 m/s
    }

    /** 현재 날씨 기반 달리기 지수 계산 */
    fun getCurrentRunningIndex(): RunningIndexResult? {
        if (!weatherClient.isAvailable()) return null

        val weather = weatherClient.getUltraSrtForecast() ?: return null
        val score = calculateIndex(weather)

        return RunningIndexResult(
                score = score,
                rating = getRating(score),
                weather =
                        WeatherSummary(
                                temp = weather.temp,
                                humidity = weather.humidity,
                                windSpeed = weather.windSpeed?.div(10.0),
                                precipType = weather.precipType,
                                sky = weather.sky
                        ),
                advice = generateAdvice(score, weather),
                warnings = getWarnings(weather)
        )
    }

    /** 시간대별 달리기 지수 계산 (오늘 기준) */
    fun getHourlyRunningIndex(): HourlyRunningIndexResult? {
        if (!weatherClient.isAvailable()) return null

        val hourlyForecast = weatherClient.getHourlyForecast()
        if (hourlyForecast.isEmpty()) return null

        val hourlyScores =
                hourlyForecast.map { hw ->
                    HourlyScore(
                            hour = hw.hour,
                            score = calculateIndexFromHourly(hw),
                            temp = hw.temp,
                            precipType = hw.precipType,
                            sky = hw.sky
                    )
                }

        val bestHour = hourlyScores.maxByOrNull { it.score }

        return HourlyRunningIndexResult(
                hourlyScores = hourlyScores,
                bestHour = bestHour?.hour,
                bestScore = bestHour?.score ?: 0,
                recommendation = generateRecommendation(hourlyScores, bestHour)
        )
    }

    /** WeatherData 기반 지수 계산 */
    private fun calculateIndex(weather: WeatherData): Int {
        var score = 100

        // 1. 기온 점수 (30%)
        weather.temp?.let { temp ->
            if (temp < OPTIMAL_TEMP_MIN) {
                score -= ((OPTIMAL_TEMP_MIN - temp) / 5) * 10
            } else if (temp > OPTIMAL_TEMP_MAX) {
                score -= ((temp - OPTIMAL_TEMP_MAX) / 5) * 10
            }
        }

        // 2. 습도 점수 (20%)
        weather.humidity?.let { humidity ->
            if (humidity < OPTIMAL_HUMIDITY_MIN) {
                score -= ((OPTIMAL_HUMIDITY_MIN - humidity) / 10) * 5
            } else if (humidity > OPTIMAL_HUMIDITY_MAX) {
                score -= ((humidity - OPTIMAL_HUMIDITY_MAX) / 10) * 5
            }
        }

        // 3. 풍속 점수 (20%)
        weather.windSpeed?.let { ws ->
            if (ws > OPTIMAL_WIND_MAX) {
                score -= ((ws - OPTIMAL_WIND_MAX) / 10) * 5
            }
        }

        // 4. 강수 점수 (20%)
        when (weather.precipType) {
            "RAIN" -> score -= 40
            "SLEET" -> score -= 45
            "SNOW" -> score -= 50
        }

        // 5. 하늘 점수 (10%)
        when (weather.sky) {
            "CLOUDY" -> score -= 10
            "PARTLY_CLOUDY" -> score -= 5
        }

        return max(0, min(100, score))
    }

    /** HourlyWeather 기반 지수 계산 */
    private fun calculateIndexFromHourly(hw: HourlyWeather): Int {
        var score = 100

        hw.temp?.let { temp ->
            if (temp < OPTIMAL_TEMP_MIN) {
                score -= ((OPTIMAL_TEMP_MIN - temp) / 5) * 10
            } else if (temp > OPTIMAL_TEMP_MAX) {
                score -= ((temp - OPTIMAL_TEMP_MAX) / 5) * 10
            }
        }

        hw.humidity?.let { humidity ->
            if (humidity < OPTIMAL_HUMIDITY_MIN) {
                score -= ((OPTIMAL_HUMIDITY_MIN - humidity) / 10) * 5
            } else if (humidity > OPTIMAL_HUMIDITY_MAX) {
                score -= ((humidity - OPTIMAL_HUMIDITY_MAX) / 10) * 5
            }
        }

        hw.windSpeed?.let { ws ->
            if (ws > OPTIMAL_WIND_MAX) {
                score -= ((ws - OPTIMAL_WIND_MAX) / 10) * 5
            }
        }

        when (hw.precipType) {
            "RAIN" -> score -= 40
            "SLEET" -> score -= 45
            "SNOW" -> score -= 50
        }

        when (hw.sky) {
            "CLOUDY" -> score -= 10
            "PARTLY_CLOUDY" -> score -= 5
        }

        return max(0, min(100, score))
    }

    private fun getRating(score: Int): String =
            when {
                score >= 80 -> "EXCELLENT"
                score >= 60 -> "GOOD"
                score >= 40 -> "FAIR"
                score >= 20 -> "POOR"
                else -> "BAD"
            }

    private fun generateAdvice(score: Int, weather: WeatherData): String =
            when {
                score >= 80 -> "🏃 최적의 러닝 날씨입니다! 오늘 달리기에 나서보세요."
                score >= 60 -> "👍 괜찮은 러닝 날씨입니다. 가볍게 달려보세요."
                score >= 40 -> "⚠️ 러닝에 다소 불리한 날씨입니다. 주의하세요."
                weather.precipType != "NONE" -> "🌧️ 비/눈 예보가 있습니다. 실내 러닝을 권장합니다."
                else -> "❌ 러닝하기 어려운 날씨입니다. 오늘은 휴식하세요."
            }

    private fun getWarnings(weather: WeatherData): List<String> {
        val warnings = mutableListOf<String>()

        weather.temp?.let { temp ->
            if (temp >= 30) warnings.add("🔥 폭염 주의: 탈수와 열사병에 주의하세요")
            if (temp <= 0) warnings.add("🥶 한파 주의: 체온 유지에 신경쓰세요")
        }

        weather.windSpeed?.let { ws -> if (ws >= 100) warnings.add("💨 강풍 주의: 풍속이 높습니다") }

        if (weather.precipType != "NONE") {
            warnings.add("🌧️ 강수 주의: 미끄러움과 시야에 주의하세요")
        }

        return warnings
    }

    private fun generateRecommendation(scores: List<HourlyScore>, best: HourlyScore?): String {
        if (best == null) return "날씨 정보를 가져올 수 없습니다."

        val hour24 = best.hour
        val hour12 = if (hour24 > 12) hour24 - 12 else if (hour24 == 0) 12 else hour24
        val ampm = if (hour24 >= 12) "오후" else "오전"

        return when {
            best.score >= 80 -> "$ampm ${hour12}시가 가장 좋습니다 (${best.score}점). 최적의 러닝 시간!"
            best.score >= 60 -> "$ampm ${hour12}시를 추천합니다 (${best.score}점)."
            best.score >= 40 -> "$ampm ${hour12}시가 그나마 낫습니다 (${best.score}점). 주의해서 달리세요."
            else -> "오늘은 전반적으로 러닝하기 어려운 날씨입니다. 실내 운동을 권장합니다."
        }
    }
}

// DTOs
data class RunningIndexResult(
        val score: Int,
        val rating: String,
        val weather: WeatherSummary,
        val advice: String,
        val warnings: List<String>
)

data class WeatherSummary(
        val temp: Int?,
        val humidity: Int?,
        val windSpeed: Double?,
        val precipType: String,
        val sky: String
)

data class HourlyRunningIndexResult(
        val hourlyScores: List<HourlyScore>,
        val bestHour: Int?,
        val bestScore: Int,
        val recommendation: String
)

data class HourlyScore(
        val hour: Int,
        val score: Int,
        val temp: Int?,
        val precipType: String,
        val sky: String
)
