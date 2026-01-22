package io.hansu.pacer.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

/** 기상청 API Hub 클라이언트: https://apihub.kma.go.kr */
@Component
class KmaWeatherClient(@Value("\${kma.auth.key:}") private val authKey: String) {
    private val om = jacksonObjectMapper()
    private val restTemplate = RestTemplate()

    companion object {
        // 기상청 API Hub endpoints
        private const val BASE_URL = "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0"
        private const val ULTRA_SRT_NCST = "/getUltraSrtNcst"  // 초단기실황 (현재 날씨)
        private const val ULTRA_SRT_FCST = "/getUltraSrtFcst"  // 초단기예보 (6시간 예보)

        // 서울 기본 좌표 (격자 X, Y)
        const val DEFAULT_NX = 60
        const val DEFAULT_NY = 127

        // 카테고리 코드
        const val CAT_TEMP = "T1H"      // 기온
        const val CAT_HUMIDITY = "REH"  // 습도
        const val CAT_WIND_SPEED = "WSD" // 풍속
        const val CAT_PRECIP_TYPE = "PTY" // 강수형태
        const val CAT_SKY = "SKY"       // 하늘상태
        const val CAT_RAIN_1H = "RN1"   // 1시간 강수량
    }

    fun isAvailable(): Boolean = authKey.isNotBlank()

    /**
     * 초단기실황 조회 (현재 관측값)
     * 매시 정각 발표, 40분 이후 조회 가능
     */
    fun getUltraSrtNcst(
            nx: Int = DEFAULT_NX,
            ny: Int = DEFAULT_NY,
            dateTime: LocalDateTime = LocalDateTime.now()
    ): WeatherData? {
        if (!isAvailable()) return null

        try {
            val baseTime = getNcstBaseTime(dateTime)
            val baseDate = if (dateTime.minute < 40 && dateTime.hour == 0) {
                dateTime.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            } else if (dateTime.minute < 40) {
                dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            } else {
                dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            }

            val uri = DefaultUriBuilderFactory()
                            .uriString("$BASE_URL$ULTRA_SRT_NCST")
                            .queryParam("authKey", authKey)
                            .queryParam("numOfRows", 10)
                            .queryParam("pageNo", 1)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build()
            println("KMA NCST URI: $uri")

            val response = restTemplate.getForObject(uri, String::class.java) ?: return null
            return parseNcstResponse(response, dateTime)
        } catch (e: Exception) {
            println("Warning: KMA API Hub 초단기실황 call failed: ${e.message}")
            return null
        }
    }

    /**
     * 현재 시각 기준 초단기예보 조회
     * @param nx X 격자 좌표
     * @param ny Y 격자 좌표
     * @param dateTime 조회 기준 시간 (기본: 현재)
     */
    fun getUltraSrtForecast(
            nx: Int = DEFAULT_NX,
            ny: Int = DEFAULT_NY,
            dateTime: LocalDateTime = LocalDateTime.now()
    ): WeatherData? {
        if (!isAvailable()) return null

        try {
            // 기상청 API는 매 정시 45분 이후에 해당 시각 데이터 제공
            // 30분 발표 기준으로 조정
            val baseTime = getBaseTime(dateTime)
            val baseDate = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            val uri = DefaultUriBuilderFactory()
                            .uriString("$BASE_URL$ULTRA_SRT_FCST")
                            .queryParam("authKey", authKey)
                            .queryParam("numOfRows", 60)
                            .queryParam("pageNo", 1)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build()
            println("KMA FCST URI: $uri")

            val response = restTemplate.getForObject(uri, String::class.java) ?: return null
            return parseResponse(response, dateTime)
        } catch (e: Exception) {
            println("Warning: KMA API Hub call failed: ${e.message}")
            return null
        }
    }

    /** 시간대별 예보 조회 (최대 6시간) */
    fun getHourlyForecast(
            nx: Int = DEFAULT_NX,
            ny: Int = DEFAULT_NY,
            hours: Int = 6
    ): List<HourlyWeather> {
        if (!isAvailable()) return emptyList()

        val now = LocalDateTime.now()
        val baseTime = getBaseTime(now)
        val baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        try {
            val uri = DefaultUriBuilderFactory()
                            .uriString("$BASE_URL$ULTRA_SRT_FCST")
                            .queryParam("authKey", authKey)
                            .queryParam("numOfRows", hours * 10) // 충분한 데이터 요청
                            .queryParam("pageNo", 1)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build()

            val response = restTemplate.getForObject(uri, String::class.java) ?: return emptyList()
            return parseHourlyResponse(response)
        } catch (e: Exception) {
            println("Warning: KMA API Hub hourly forecast call failed: ${e.message}")
            return emptyList()
        }
    }

    private fun getNcstBaseTime(dateTime: LocalDateTime): String {
        // 초단기실황은 매시 정각 발표, 40분 이후 조회 가능
        val hour = dateTime.hour
        val minute = dateTime.minute

        val baseHour = if (minute >= 40) hour else (hour - 1 + 24) % 24
        return String.format("%02d00", baseHour)
    }

    private fun getBaseTime(dateTime: LocalDateTime): String {
        // 초단기예보는 매시 30분마다 발표, 45분 이후 조회 가능
        val hour = dateTime.hour
        val minute = dateTime.minute

        val baseHour = if (minute >= 45) hour else (hour - 1 + 24) % 24
        return String.format("%02d30", baseHour)
    }

    private fun parseNcstResponse(json: String, targetTime: LocalDateTime): WeatherData? {
        try {
            val root = om.readTree(json)
            val items = root.path("response").path("body").path("items").path("item")

            if (!items.isArray || items.isEmpty) return null

            var temp: Int? = null
            var humidity: Int? = null
            var windSpeed: Int? = null
            var precipType: String? = null

            for (item in items) {
                val category = item.path("category").asText()
                val value = item.path("obsrValue").asText()  // 실황은 obsrValue 사용

                when (category) {
                    CAT_TEMP -> temp = value.toDoubleOrNull()?.toInt()
                    CAT_HUMIDITY -> humidity = value.toIntOrNull()
                    CAT_WIND_SPEED -> windSpeed = (value.toDoubleOrNull()?.times(10))?.toInt()
                    CAT_PRECIP_TYPE -> precipType = parsePrecipType(value)
                }
            }

            return WeatherData(
                    temp = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    precipType = precipType ?: "NONE",
                    sky = "CLEAR",  // 초단기실황에는 SKY 없음, 기본값 사용
                    observedAt = targetTime
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseResponse(json: String, targetTime: LocalDateTime): WeatherData? {
        try {
            val root = om.readTree(json)
            val items = root.path("response").path("body").path("items").path("item")

            if (!items.isArray || items.isEmpty) return null

            val targetHour = String.format("%02d00", (targetTime.hour + 1) % 24)

            var temp: Int? = null
            var humidity: Int? = null
            var windSpeed: Int? = null
            var precipType: String? = null
            var sky: String? = null

            for (item in items) {
                val fcstTime = item.path("fcstTime").asText()
                if (fcstTime != targetHour) continue

                val category = item.path("category").asText()
                val value = item.path("fcstValue").asText()

                when (category) {
                    CAT_TEMP -> temp = value.toIntOrNull()
                    CAT_HUMIDITY -> humidity = value.toIntOrNull()
                    CAT_WIND_SPEED -> windSpeed = (value.toDoubleOrNull()?.times(10))?.toInt()
                    CAT_PRECIP_TYPE -> precipType = parsePrecipType(value)
                    CAT_SKY -> sky = parseSky(value)
                }
            }

            return WeatherData(
                    temp = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    precipType = precipType ?: "NONE",
                    sky = sky ?: "CLEAR",
                    observedAt = targetTime
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseHourlyResponse(json: String): List<HourlyWeather> {
        try {
            val root = om.readTree(json)
            val items = root.path("response").path("body").path("items").path("item")

            if (!items.isArray || items.isEmpty) return emptyList()

            // 시간별로 그룹화
            val hourlyMap = mutableMapOf<String, MutableMap<String, String>>()

            for (item in items) {
                val fcstTime = item.path("fcstTime").asText()
                val category = item.path("category").asText()
                val value = item.path("fcstValue").asText()

                hourlyMap.getOrPut(fcstTime) { mutableMapOf() }[category] = value
            }

            return hourlyMap
                    .map { (time, data) ->
                        HourlyWeather(
                                hour = time.take(2).toIntOrNull() ?: 0,
                                temp = data[CAT_TEMP]?.toIntOrNull(),
                                humidity = data[CAT_HUMIDITY]?.toIntOrNull(),
                                windSpeed =
                                        data[CAT_WIND_SPEED]?.toDoubleOrNull()?.times(10)?.toInt(),
                                precipType = parsePrecipType(data[CAT_PRECIP_TYPE] ?: "0"),
                                sky = parseSky(data[CAT_SKY] ?: "1")
                        )
                    }
                    .sortedBy { it.hour }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parsePrecipType(code: String): String =
            when (code) {
                "0" -> "NONE"
                "1" -> "RAIN"
                "2" -> "SLEET"
                "3" -> "SNOW"
                "5" -> "RAIN" // 빗방울
                "6" -> "SLEET" // 빗방울눈날림
                "7" -> "SNOW" // 눈날림
                else -> "NONE"
            }

    private fun parseSky(code: String): String =
            when (code) {
                "1" -> "CLEAR"
                "3" -> "PARTLY_CLOUDY"
                "4" -> "CLOUDY"
                else -> "CLEAR"
            }
}

data class WeatherData(
        val temp: Int?, // 기온 (°C)
        val humidity: Int?, // 습도 (%)
        val windSpeed: Int?, // 풍속 (m/s × 10)
        val precipType: String, // 강수형태
        val sky: String, // 하늘상태
        val observedAt: LocalDateTime
)

data class HourlyWeather(
        val hour: Int,
        val temp: Int?,
        val humidity: Int?,
        val windSpeed: Int?,
        val precipType: String,
        val sky: String
)
