package day.domain

interface LocationRepository {
    suspend fun getChoice(): LocationChoice

    suspend fun setChoice(choice: LocationChoice)

    suspend fun listSavedLocations(): List<SavedLocation>

    suspend fun resolveActivePoint(): GeoPoint

    suspend fun getActiveLabel(): String

    suspend fun saveLocation(label: String, point: GeoPoint, isCurrent: Boolean)

    suspend fun hasLocation(): Boolean
}
