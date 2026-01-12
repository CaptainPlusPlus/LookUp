package day.data

import day.domain.DeviceLocationRepository
import day.domain.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class DeviceLocationRepositoryImpl : DeviceLocationRepository {
    private val locationManager = CLLocationManager()

    override suspend fun getCurrentLocation(): GeoPoint? {
        val status = CLLocationManager.authorizationStatus()
        if (status != kCLAuthorizationStatusAuthorizedWhenInUse &&
            status != kCLAuthorizationStatusAuthorizedAlways) {
            locationManager.requestWhenInUseAuthorization()
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    val location = didUpdateLocations.firstOrNull() as? CLLocation
                    location?.let { loc ->
                        val geoPoint = loc.coordinate.useContents {
                            GeoPoint(latitude, longitude)
                        }
                        continuation.resume(geoPoint)
                    } ?: continuation.resume(null)
                    manager.stopUpdatingLocation()
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: platform.Foundation.NSError
                ) {
                    continuation.resume(null)
                    manager.stopUpdatingLocation()
                }
            }

            locationManager.delegate = delegate
            locationManager.requestLocation()

            continuation.invokeOnCancellation {
                locationManager.stopUpdatingLocation()
            }
        }
    }
}
