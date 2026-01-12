package day.data.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class PreferencesStorageImpl : PreferencesStorage, KoinComponent {
    private val context: Context by inject()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val json = Json

    override suspend fun getCurrentLocation(): LocationEntity? {
        val jsonStr = prefs.getString(KEY_CURRENT_LOCATION, null) ?: return null
        return try {
            json.decodeFromString<LocationEntity>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        val jsonStr = json.encodeToString(location.copy(isCurrent = true))
        prefs.edit().putString(KEY_CURRENT_LOCATION, jsonStr).apply()
    }

    override suspend fun clearCurrentLocation() {
        prefs.edit().remove(KEY_CURRENT_LOCATION).apply()
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        return listOfNotNull(getCurrentLocation())
    }

    companion object {
        private const val PREFS_NAME = "lookup_prefs"
        private const val KEY_CURRENT_LOCATION = "current_location"
    }
}
