package day.data

import day.domain.GeoPoint
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SunRepositoryImplTest {

    @Test
    fun testGetSunAngleNowDeg_Success() = runBlocking {
        // Use current date for timestamps to ensure the time is within the sunrise-sunset window
        val now = kotlinx.datetime.Clock.System.now()
        val today = now.toString().substringBefore("T")

        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "results": {
                            "sunrise": "${today}T06:00:00+00:00",
                            "sunset": "${today}T18:00:00+00:00",
                            "solar_noon": "${today}T12:00:00+00:00",
                            "day_length": 43200
                        },
                        "status": "OK"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val repository = SunRepositoryImpl(httpClient)
        val angle = repository.getSunAngleNowDeg(GeoPoint(0.0, 0.0))

        // Verify angle is valid (0-180 degrees)
        // 0° = sunrise (right), 90° = noon (center), 180° = sunset (left)
        kotlin.test.assertTrue(angle >= 0f && angle <= 180f, "Sun angle should be between 0 and 180 degrees, got $angle")
    }

    @Test
    fun testGetSunAngleNowDeg_Error() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status": "INVALID_REQUEST"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val repository = SunRepositoryImpl(httpClient)
        val angle = repository.getSunAngleNowDeg(GeoPoint(0.0, 0.0))

        assertEquals(45f, angle)
    }
}
