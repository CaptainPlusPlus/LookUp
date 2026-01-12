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
            val response: OpenMeteoResponse = httpClient.get(BASE_URL) {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current", CURRENT_PARAMS)
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
        val isPrecipOrStorm = weatherCode != null && weatherCode >= PRECIP_THRESHOLD
        if (cloudCover <= COVER_MIN_THRESHOLD && !isPrecipOrStorm) {
            return emptyList()
        }

        val types = LinkedHashSet<CloudType>()

        if (isPrecipOrStorm) {
            types.add(CloudType.NIMBUS)
        }

        if (weatherCode == WEATHER_CODE_FOG || weatherCode == WEATHER_CODE_FOG_DEPOSIT) {
            types.add(CloudType.STRATUS)
        }

        if (cloudHigh >= HIGH_CLOUD_THRESHOLD) {
            types.add(CloudType.CIRRUS)
        }

        if (cloudMid >= MID_CLOUD_THRESHOLD) {
            if (cloudMid >= MID_CLOUD_HEAVY_THRESHOLD || weatherCode == WEATHER_CODE_OVERCAST) {
                types.add(CloudType.STRATUS)
            } else {
                types.add(CloudType.CUMULUS)
            }
        }

        if (cloudLow >= LOW_CLOUD_THRESHOLD) {
            if (cloudLow >= LOW_CLOUD_HEAVY_THRESHOLD || weatherCode == WEATHER_CODE_OVERCAST) {
                types.add(CloudType.STRATUS)
            } else {
                types.add(CloudType.CUMULUS)
            }
        }

        if (types.isEmpty() && cloudCover > COVER_MIN_THRESHOLD) {
            if (cloudCover > COVER_FALLBACK_THRESHOLD) types.add(CloudType.STRATUS)
            else types.add(CloudType.CUMULUS)
        }

        val orderedResult = mutableListOf<CloudType>()
        if (types.contains(CloudType.NIMBUS)) orderedResult.add(CloudType.NIMBUS)
        if (types.contains(CloudType.STRATUS)) orderedResult.add(CloudType.STRATUS)
        if (types.contains(CloudType.CUMULUS)) orderedResult.add(CloudType.CUMULUS)
        if (types.contains(CloudType.CIRRUS)) orderedResult.add(CloudType.CIRRUS)

        return orderedResult.take(MAX_CLOUD_TYPES)
    }

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private const val CURRENT_PARAMS = "weather_code,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high"
        private const val PRECIP_THRESHOLD = 51
        private const val COVER_MIN_THRESHOLD = 0
        private const val WEATHER_CODE_FOG = 45
        private const val WEATHER_CODE_FOG_DEPOSIT = 48
        private const val WEATHER_CODE_OVERCAST = 3
        private const val HIGH_CLOUD_THRESHOLD = 20
        private const val MID_CLOUD_THRESHOLD = 25
        private const val MID_CLOUD_HEAVY_THRESHOLD = 60
        private const val LOW_CLOUD_THRESHOLD = 25
        private const val LOW_CLOUD_HEAVY_THRESHOLD = 50
        private const val COVER_FALLBACK_THRESHOLD = 50
        private const val MAX_CLOUD_TYPES = 4
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
