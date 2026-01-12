package day.data

import day.domain.CitySearchRepository
import day.domain.CitySearchResult
import day.domain.GeoPoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class NominatimResult(
    val place_id: Long,
    val display_name: String,
    val lat: String,
    val lon: String
)

class CitySearchRepositoryImpl(private val httpClient: HttpClient) : CitySearchRepository {
    override suspend fun searchCity(query: String): List<CitySearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val results: List<NominatimResult> = httpClient.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", 5)
                header("User-Agent", "LookUpApp")
            }.body()

            results.map {
                CitySearchResult(
                    id = it.place_id.toString(),
                    label = it.display_name,
                    point = GeoPoint(it.lat.toDouble(), it.lon.toDouble())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
