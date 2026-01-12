package day.data.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

actual class PreferencesStorageImpl : PreferencesStorage {
    private val prefs = Preferences.userRoot().node(PREFS_NODE)
    private val json = Json

    override suspend fun getCurrentLocation(): LocationEntity? {
        val jsonStr = prefs.get(KEY_CURRENT_LOCATION, null) ?: return null
        return try {
            json.decodeFromString<LocationEntity>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        val jsonStr = json.encodeToString(location.copy(isCurrent = true))
        prefs.put(KEY_CURRENT_LOCATION, jsonStr)
        prefs.flush()
    }

    override suspend fun clearCurrentLocation() {
        prefs.remove(KEY_CURRENT_LOCATION)
        prefs.flush()
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        return listOfNotNull(getCurrentLocation())
    }

    companion object {
        private const val PREFS_NODE = "com.lookup.app"
        private const val KEY_CURRENT_LOCATION = "current_location"
    }
}
