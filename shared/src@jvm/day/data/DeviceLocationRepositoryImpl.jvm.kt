package day.data

import day.domain.DeviceLocationRepository
import day.domain.GeoPoint

actual class DeviceLocationRepositoryImpl : DeviceLocationRepository {
    override suspend fun getCurrentLocation(): GeoPoint? {
        // JVM/Desktop doesn't have GPS - return mock London coordinates
        return GeoPoint(51.5074, -0.1278)
    }
}
