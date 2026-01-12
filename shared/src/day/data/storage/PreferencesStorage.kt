package day.data.storage

interface PreferencesStorage {
    suspend fun getCurrentLocation(): LocationEntity?
    suspend fun saveCurrentLocation(location: LocationEntity)
    suspend fun clearCurrentLocation()
    suspend fun getAllLocations(): List<LocationEntity>
}
