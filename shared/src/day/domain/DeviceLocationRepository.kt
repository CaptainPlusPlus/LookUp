package day.domain

interface DeviceLocationRepository {
    suspend fun getCurrentLocation(): GeoPoint?
}
