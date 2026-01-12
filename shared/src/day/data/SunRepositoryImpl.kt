package day.data

import day.domain.GeoPoint
import day.domain.SunEvents
import day.domain.SunRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SunRepositoryImpl(
    private val httpClient: HttpClient
) : SunRepository {
    override suspend fun getSunAngleNowDeg(at: GeoPoint): Float {
        val results = fetchSunResults(at) ?: return FALLBACK_ANGLE
        return calculateSunAngle(results)
    }

    override suspend fun getSunEvents(at: GeoPoint, date: String?): SunEvents {
        val results = fetchSunResults(at, date) ?: throw Exception("Failed to fetch sun events")
        return SunEvents(
            sunrise = Instant.parse(results.sunrise),
            sunset = Instant.parse(results.sunset)
        )
    }

    private suspend fun fetchSunResults(at: GeoPoint, date: String? = null): SunriseSunsetResults? {
        return try {
            val response: SunriseSunsetResponse = httpClient.get(BASE_URL) {
                parameter("lat", at.latitudeDeg)
                parameter("lng", at.longitudeDeg)
                parameter("formatted", UNFORMATTED_PARAM)
                date?.let { parameter("date", it) }
            }.body()

            if (response.status == OK_STATUS) {
                response.results
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSunAngle(results: SunriseSunsetResults): Float {
        return try {
            val sunrise = Instant.parse(results.sunrise).toEpochMilliseconds()
            val sunset = Instant.parse(results.sunset).toEpochMilliseconds()
            val now = Clock.System.now().toEpochMilliseconds()

            when {
                now < sunrise -> ANGLE_MIN
                now > sunset -> ANGLE_MAX
                else -> {
                    val progress = (now - sunrise).toFloat() / (sunset - sunrise).toFloat()
                    (progress * ANGLE_MAX).coerceIn(ANGLE_MIN, ANGLE_MAX)
                }
            }
        } catch (e: Exception) {
            ANGLE_CENTER
        }
    }

    companion object {
        private const val BASE_URL = "https://api.sunrise-sunset.org/json"
        private const val OK_STATUS = "OK"
        private const val UNFORMATTED_PARAM = 0
        private const val FALLBACK_ANGLE = 45f
        private const val ANGLE_MIN = 0f
        private const val ANGLE_MAX = 180f
        private const val ANGLE_CENTER = 90f
    }
}

@Serializable
data class SunriseSunsetResponse(
    val results: SunriseSunsetResults,
    val status: String
)

@Serializable
data class SunriseSunsetResults(
    val sunrise: String,
    val sunset: String,
    @SerialName("solar_noon")
    val solarNoon: String,
    @SerialName("day_length")
    val dayLength: Int
)
