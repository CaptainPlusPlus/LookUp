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
        context.getSharedPreferences("lookup_prefs", Context.MODE_PRIVATE)
    }
    private val json = Json

    override suspend fun getCurrentLocation(): LocationEntity? {
        val jsonStr = prefs.getString("current_location", null) ?: return null
        return try {
            json.decodeFromString<LocationEntity>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        val jsonStr = json.encodeToString(location.copy(isCurrent = true))
        prefs.edit().putString("current_location", jsonStr).apply()
    }

    override suspend fun clearCurrentLocation() {
        prefs.edit().remove("current_location").apply()
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        // For simplicity, we only store the current location
        return listOfNotNull(getCurrentLocation())
    }
}
