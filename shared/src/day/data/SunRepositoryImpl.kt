package day.data

import day.domain.GeoPoint
import day.domain.SunEvents
import day.domain.SunRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SunRepositoryImpl(
    private val httpClient: HttpClient
) : SunRepository {
    override suspend fun getSunAngleNowDeg(at: GeoPoint): Float {
        val results = fetchSunResults(at) ?: return 45f
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
            val response: SunriseSunsetResponse = httpClient.get("https://api.sunrise-sunset.org/json") {
                parameter("lat", at.latitudeDeg)
                parameter("lng", at.longitudeDeg)
                parameter("formatted", 0)
                date?.let { parameter("date", it) }
            }.body()

            if (response.status == "OK") {
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
            // Parse ISO 8601 timestamps using kotlinx-datetime
            val sunrise = Instant.parse(results.sunrise).toEpochMilliseconds()
            val sunset = Instant.parse(results.sunset).toEpochMilliseconds()
            val now = Clock.System.now().toEpochMilliseconds()

            when {
                now < sunrise -> {
                    // Before sunrise - sun is below horizon (far right)
                    0f
                }
                now > sunset -> {
                    // After sunset - sun is below horizon (far left)
                    180f
                }
                else -> {
                    // During the day: sunrise (0°) to sunset (180°)
                    // Linear interpolation across the full day
                    val progress = (now - sunrise).toFloat() / (sunset - sunrise).toFloat()
                    (progress * 180f).coerceIn(0f, 180f)
                }
            }
        } catch (e: Exception) {
            90f // Default fallback (center)
        }
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
