package day.data

import day.data.storage.LocationEntity
import day.data.storage.PreferencesStorage
import day.domain.GeoPoint
import day.domain.LocationChoice
import day.domain.LocationRepository
import day.domain.SavedLocation

class LocationRepositoryImpl(private val storage: PreferencesStorage) : LocationRepository {
    override suspend fun getChoice(): LocationChoice {
        val current = storage.getCurrentLocation()
        return if (current != null) {
            LocationChoice.Saved(current.id)
        } else {
            LocationChoice.Device
        }
    }

    override suspend fun setChoice(choice: LocationChoice) {
        when (choice) {
            is LocationChoice.Saved -> {
            }
            LocationChoice.Device -> {
                storage.clearCurrentLocation()
            }
        }
    }

    override suspend fun listSavedLocations(): List<SavedLocation> {
        return storage.getAllLocations().map {
            SavedLocation(it.id, it.label, GeoPoint(it.latitudeDeg, it.longitudeDeg))
        }
    }

    override suspend fun resolveActivePoint(): GeoPoint {
        val current = storage.getCurrentLocation()
        return if (current != null) {
            GeoPoint(current.latitudeDeg, current.longitudeDeg)
        } else {
            GeoPoint(0.0, 0.0)
        }
    }

    override suspend fun getActiveLabel(): String {
        return storage.getCurrentLocation()?.label ?: "Unknown"
    }

    override suspend fun saveLocation(label: String, point: GeoPoint, isCurrent: Boolean) {
        val entity = LocationEntity(
            id = label + point.latitudeDeg + point.longitudeDeg,
            label = label,
            latitudeDeg = point.latitudeDeg,
            longitudeDeg = point.longitudeDeg,
            isCurrent = isCurrent
        )
        if (isCurrent) {
            storage.saveCurrentLocation(entity)
        }
    }

    override suspend fun hasLocation(): Boolean {
        return storage.getCurrentLocation() != null
    }

    override suspend fun clearLocation() {
        storage.clearCurrentLocation()
    }
}
