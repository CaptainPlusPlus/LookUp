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
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "results": {
                            "sunrise": "2026-01-11T07:00:00+00:00",
                            "sunset": "2026-01-11T17:00:00+00:00",
                            "solar_noon": "2026-01-11T12:00:00+00:00",
                            "day_length": 36000
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

        assertEquals(90f, angle)
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
