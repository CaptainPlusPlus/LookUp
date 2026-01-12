package day.domain

interface SunRepository {
    suspend fun getSunAngleNowDeg(at: GeoPoint): Float // 0..180
}
