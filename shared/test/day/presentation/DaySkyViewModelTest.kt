package day.presentation

import day.domain.GeoPoint
import day.domain.GetSunAngle
import day.domain.LocationChoice
import day.domain.LocationRepository
import day.domain.SavedLocation
import day.domain.SunRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeLocationRepositoryForDaySky : LocationRepository {
    var mockLocation = SavedLocation("london", "London", GeoPoint(51.5074, -0.1278))

    override suspend fun getChoice(): LocationChoice {
        return LocationChoice.Saved(mockLocation.id)
    }

    override suspend fun setChoice(choice: LocationChoice) {}

    override suspend fun listSavedLocations(): List<SavedLocation> {
        return listOf(mockLocation)
    }

    override suspend fun resolveActivePoint(): GeoPoint {
        return mockLocation.point
    }

    override suspend fun getActiveLabel(): String {
        return mockLocation.label
    }

    override suspend fun saveLocation(label: String, point: GeoPoint, isCurrent: Boolean) {}

    override suspend fun hasLocation(): Boolean {
        return true
    }

    override suspend fun clearLocation() {
        mockLocation = SavedLocation("", "", GeoPoint(0.0, 0.0))
    }
}

class FakeSunRepositoryForDaySky(private val mockAngle: Float = 45f) : SunRepository {
    override suspend fun getSunAngleNowDeg(at: GeoPoint): Float {
        return mockAngle
    }
}

class DaySkyViewModelTest {
    @Test
    fun testRefresh_LoadsLocationAndSunAngle() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 75f)
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        assertEquals(75f, state.sunAngleDeg)
        assertEquals("London", state.location?.label)
        assertEquals(51.5074, state.location?.latitudeDeg)
        assertEquals(-0.1278, state.location?.longitudeDeg)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun testRefresh_WithDifferentLocation() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        locationRepo.mockLocation = SavedLocation("tokyo", "Tokyo", GeoPoint(35.6762, 139.6503))
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 120f)
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        assertEquals(120f, state.sunAngleDeg)
        assertEquals("Tokyo", state.location?.label)
        assertEquals(35.6762, state.location?.latitudeDeg)
        assertEquals(139.6503, state.location?.longitudeDeg)
    }

    @Test
    fun testOnSunTapped_SetsExpanded() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        // Wait for init
        delay(100)

        viewModel.onSunTapped()

        val state = viewModel.state.value
        assertEquals(true, state.isExpanded)
    }

    @Test
    fun testOnBackTapped_CollapsesExpanded() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        // Wait for init
        delay(100)

        // First expand
        viewModel.onSunTapped()
        assertEquals(true, viewModel.state.value.isExpanded)

        // Then collapse
        viewModel.onBackTapped()
        assertEquals(false, viewModel.state.value.isExpanded)
    }

    @Test
    fun testGetSunAngle_UsesActualLocation() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        // Set specific location
        locationRepo.mockLocation = SavedLocation("paris", "Paris", GeoPoint(48.8566, 2.3522))
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 90f)
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        // Verify the location used for the sun angle calculation
        assertEquals("Paris", state.location?.label)
        assertEquals(48.8566, state.location?.latitudeDeg)
        assertEquals(2.3522, state.location?.longitudeDeg)
        // Verify the sun angle was calculated
        assertEquals(90f, state.sunAngleDeg)
    }

    @Test
    fun testOnChangeLocationClicked_ClearsLocationAndNavigates() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo)

        var navigated = false
        viewModel.onChangeLocationClicked {
            navigated = true
        }

        assertEquals(true, navigated)
        assertEquals("", locationRepo.mockLocation.label)
    }
}
