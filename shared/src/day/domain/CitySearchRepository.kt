package day.domain

data class CitySearchResult(
    val id: String,
    val label: String,
    val point: GeoPoint
)

interface CitySearchRepository {
    suspend fun searchCity(query: String): List<CitySearchResult>
}
