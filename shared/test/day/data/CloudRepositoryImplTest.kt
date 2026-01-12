package day.data

import day.domain.CloudType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudRepositoryImplTest {

    private val httpClient = HttpClient(MockEngine { respondOk("") }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val repository = CloudRepositoryImpl(httpClient)

    @Test
    fun testClassify_ClearSky() {
        // total=5, low=2, mid=2, high=1, weather=0 -> []
        val result = repository.classify(5, 2, 2, 1, 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testClassify_RainLowCloud() {
        // total=5, low=5, mid=0, high=0, weather=61 (Rain) -> [NIMBUS]
        val result = repository.classify(5, 5, 0, 0, 61)
        assertTrue(result.isNotEmpty())
        assertEquals(CloudType.NIMBUS, result.first())
    }

    @Test
    fun testClassify_LightClouds() {
        // total=20, low=20, mid=0, high=0, weather=1 -> [CUMULUS]
        val result = repository.classify(20, 20, 0, 0, 1)
        assertTrue(result.contains(CloudType.CUMULUS))
    }

    @Test
    fun testClassify_CirrusOnly() {
        // total=25, low=0, mid=0, high=25, weather=1 -> [CIRRUS]
        val result = repository.classify(25, 0, 0, 25, 1)
        assertEquals(CloudType.CIRRUS, result.first())
    }

    @Test
    fun testClassify_HeavyStratus() {
        // total=85, low=60, mid=0, high=0, weather=3 (Overcast) -> [STRATUS]
        val result = repository.classify(85, 60, 0, 0, 3)
        assertEquals(CloudType.STRATUS, result.first())
    }

    @Test
    fun testClassify_MixedClouds() {
        // total=70, low=30, mid=30, high=30, weather=1
        val result = repository.classify(70, 30, 30, 30, 1)
        assertTrue(result.contains(CloudType.STRATUS)) // from fallback or components
        assertTrue(result.contains(CloudType.CUMULUS))
        assertTrue(result.contains(CloudType.CIRRUS))
    }

    @Test
    fun testClassify_Thunderstorm() {
        // weather=95 -> starts with NIMBUS
        val result = repository.classify(60, 20, 20, 20, 95)
        assertEquals(CloudType.NIMBUS, result.first())
    }

    @Test
    fun testClassify_AtmosphereFog() {
        // weather=45 (Fog) -> Stratus
        val result = repository.classify(100, 100, 100, 100, 45)
        assertTrue(result.contains(CloudType.STRATUS))
    }

    @Test
    fun testClassify_MaximumFourTypes() {
        val result = repository.classify(100, 100, 100, 100, 95)
        assertTrue(result.size <= 4)
    }

    @Test
    fun testClassify_Ordering_NimbusFirst() {
        val result = repository.classify(50, 50, 50, 50, 95)
        assertEquals(CloudType.NIMBUS, result[0])
    }

    @Test
    fun testClassify_Prominence_StratusOverCumulus() {
        // Mixed clouds, both mid and low meet criteria for Stratus and Cumulus
        // e.g. mid=70 (Stratus), low=30 (Cumulus)
        val result = repository.classify(100, 30, 70, 0, 1)
        assertEquals(CloudType.STRATUS, result[0], "Stratus should be more prominent than Cumulus")
    }

    @Test
    fun testClassify_Prominence_CumulusOverCirrus() {
        // low=30 (Cumulus), high=80 (Cirrus)
        val result = repository.classify(100, 30, 0, 80, 1)
        assertEquals(CloudType.CUMULUS, result[0], "Cumulus should be more prominent than Cirrus")
    }

    @Test
    fun testGetCloudTypes_Success() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "current": {
                            "weather_code": 1,
                            "cloud_cover": 20,
                            "cloud_cover_low": 20,
                            "cloud_cover_mid": 0,
                            "cloud_cover_high": 0
                        }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repo = CloudRepositoryImpl(client)
        val result = repo.getCloudTypes(0.0, 0.0)
        
        assertTrue(result.isSuccess)
        val cloudResult = result.getOrNull()!!
        assertTrue(cloudResult.types.contains(CloudType.CUMULUS))
        assertEquals(20, cloudResult.inputs.cloudCover)
        assertEquals(1, cloudResult.inputs.weatherCode)
    }

    @Test
    fun testGetCloudTypes_Failure() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repo = CloudRepositoryImpl(client)
        val result = repo.getCloudTypes(0.0, 0.0)
        
        assertTrue(result.isFailure)
    }
}
