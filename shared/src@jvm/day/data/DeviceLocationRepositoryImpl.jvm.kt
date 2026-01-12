package day.data

import day.domain.DeviceLocationRepository
import day.domain.GeoPoint

actual class DeviceLocationRepositoryImpl : DeviceLocationRepository {
    override suspend fun getCurrentLocation(): GeoPoint? {
        return GeoPoint(DEFAULT_LAT, DEFAULT_LON)
    }

    companion object {
        private const val DEFAULT_LAT = 51.5074
        private const val DEFAULT_LON = -0.1278
    }
}
