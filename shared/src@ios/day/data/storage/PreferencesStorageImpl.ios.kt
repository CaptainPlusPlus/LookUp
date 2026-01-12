package day.data.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual class PreferencesStorageImpl : PreferencesStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json
    private val key = "current_location"

    override suspend fun getCurrentLocation(): LocationEntity? {
        val jsonStr = userDefaults.stringForKey(key) ?: return null
        return try {
            json.decodeFromString<LocationEntity>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        val jsonStr = json.encodeToString(location.copy(isCurrent = true))
        userDefaults.setObject(jsonStr, forKey = key)
    }

    override suspend fun clearCurrentLocation() {
        userDefaults.removeObjectForKey(key)
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        return listOfNotNull(getCurrentLocation())
    }
}
