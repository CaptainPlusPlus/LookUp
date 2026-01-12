package day.domain

interface SunRepository {
    suspend fun getSunAngleNowDeg(at: GeoPoint): Float // 0..180
    suspend fun getSunEvents(at: GeoPoint, date: String? = null): SunEvents
}

data class SunEvents(
    val sunrise: kotlinx.datetime.Instant,
    val sunset: kotlinx.datetime.Instant
)
