package day.data.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

actual class PreferencesStorageImpl : PreferencesStorage {
    private val prefs = Preferences.userRoot().node("com.lookup.app")
    private val json = Json
    private val key = "current_location"

    override suspend fun getCurrentLocation(): LocationEntity? {
        val jsonStr = prefs.get(key, null) ?: return null
        return try {
            json.decodeFromString<LocationEntity>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        val jsonStr = json.encodeToString(location.copy(isCurrent = true))
        prefs.put(key, jsonStr)
        prefs.flush()
    }

    override suspend fun clearCurrentLocation() {
        prefs.remove(key)
        prefs.flush()
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        return listOfNotNull(getCurrentLocation())
    }
}
