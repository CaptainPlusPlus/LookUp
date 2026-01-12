package day.data

import day.domain.CloudRepository
import day.domain.CloudResult
import day.domain.CloudType
import day.domain.InputsUsed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CloudRepositoryImpl(
    private val httpClient: HttpClient
) : CloudRepository {

    override suspend fun getCloudTypes(lat: Double, lon: Double): Result<CloudResult> {
        return runCatching {
            val response: OpenMeteoResponse = httpClient.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current", "weather_code,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high")
            }.body()

            val current = response.current
            val types = classify(
                current.cloudCover,
                current.cloudCoverLow,
                current.cloudCoverMid,
                current.cloudCoverHigh,
                current.weatherCode
            )
            CloudResult(types, InputsUsed(current.cloudCover, current.weatherCode))
        }
    }

    fun classify(
        cloudCover: Int,
        cloudLow: Int,
        cloudMid: Int,
        cloudHigh: Int,
        weatherCode: Int?
    ): List<CloudType> {
        // 15% threshold for "NOTHING" instead of 10%
        val isPrecipOrStorm = weatherCode != null && weatherCode >= 51
        if (cloudCover <= 15 && !isPrecipOrStorm) {
            return emptyList()
        }

        val types = LinkedHashSet<CloudType>()

        // A) Precipitation/storm dominates → Nimbus
        if (isPrecipOrStorm) {
            types.add(CloudType.NIMBUS)
        }

        // B) Atmosphere (Fog) -> Stratus
        if (weatherCode == 45 || weatherCode == 48) {
            types.add(CloudType.STRATUS)
        }

        // C) High Clouds -> Cirrus
        if (cloudHigh >= 20) {
            types.add(CloudType.CIRRUS)
        }

        // D) Mid Clouds -> Stratus or Cumulus
        if (cloudMid >= 25) {
            if (cloudMid >= 60 || weatherCode == 3) {
                types.add(CloudType.STRATUS)
            } else {
                types.add(CloudType.CUMULUS)
            }
        }

        // E) Low Clouds -> Stratus or Cumulus
        if (cloudLow >= 25) {
            if (cloudLow >= 50 || weatherCode == 3) {
                types.add(CloudType.STRATUS)
            } else {
                types.add(CloudType.CUMULUS)
            }
        }

        // F) Fallback if total cover is high but components are low
        if (types.isEmpty() && cloudCover > 15) {
            if (cloudCover > 50) types.add(CloudType.STRATUS)
            else types.add(CloudType.CUMULUS)
        }

        // Ordering: NIMBUS > STRATUS > CUMULUS > CIRRUS
        val orderedResult = mutableListOf<CloudType>()
        if (types.contains(CloudType.NIMBUS)) orderedResult.add(CloudType.NIMBUS)
        if (types.contains(CloudType.STRATUS)) orderedResult.add(CloudType.STRATUS)
        if (types.contains(CloudType.CUMULUS)) orderedResult.add(CloudType.CUMULUS)
        if (types.contains(CloudType.CIRRUS)) orderedResult.add(CloudType.CIRRUS)

        return orderedResult.take(4)
    }
}

@Serializable
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("cloud_cover")
    val cloudCover: Int,
    @SerialName("cloud_cover_low")
    val cloudCoverLow: Int,
    @SerialName("cloud_cover_mid")
    val cloudCoverMid: Int,
    @SerialName("cloud_cover_high")
    val cloudCoverHigh: Int
)
