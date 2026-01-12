package welcome.presentation

import day.domain.CitySearchRepository
import day.domain.CitySearchResult
import day.domain.DeviceLocationRepository
import day.domain.GeoPoint
import day.domain.LocationChoice
import day.domain.LocationRepository
import day.domain.SavedLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeCitySearchRepository : CitySearchRepository {
    var results: List<CitySearchResult> = emptyList()
    var lastQuery: String = ""

    override suspend fun searchCity(query: String): List<CitySearchResult> {
        lastQuery = query
        return results
    }
}

class FakeDeviceLocationRepository : DeviceLocationRepository {
    var location: GeoPoint? = null

    override suspend fun getCurrentLocation(): GeoPoint? {
        return location
    }
}

class FakeLocationRepositoryForWelcome : LocationRepository {
    var savedLabel: String? = null
    var savedPoint: GeoPoint? = null
    var hasLocationValue: Boolean = false

    override suspend fun getChoice(): LocationChoice = LocationChoice.Device
    override suspend fun setChoice(choice: LocationChoice) {}
    override suspend fun listSavedLocations(): List<SavedLocation> = emptyList()
    override suspend fun resolveActivePoint(): GeoPoint = GeoPoint(0.0, 0.0)
    override suspend fun getActiveLabel(): String = ""
    override suspend fun saveLocation(label: String, point: GeoPoint, isCurrent: Boolean) {
        savedLabel = label
        savedPoint = point
        hasLocationValue = isCurrent
    }
    override suspend fun hasLocation(): Boolean = hasLocationValue
    override suspend fun clearLocation() {
        savedLabel = null
        savedPoint = null
        hasLocationValue = false
    }
}

class WelcomeViewModelTest {
    @Test
    fun testOnSearchQueryChanged_UpdatesState() {
        val viewModel = WelcomeViewModel(FakeCitySearchRepository(), FakeDeviceLocationRepository(), FakeLocationRepositoryForWelcome())
        viewModel.onSearchQueryChanged("Tel Aviv")
        assertEquals("Tel Aviv", viewModel.state.value.searchQuery)
    }

    @Test
    fun testOnCitySelected_SavesLocationAndUpdatesState() = runBlocking {
        val locationRepo = FakeLocationRepositoryForWelcome()
        val viewModel = WelcomeViewModel(FakeCitySearchRepository(), FakeDeviceLocationRepository(), locationRepo)
        val city = CitySearchResult("1", "Tel Aviv", GeoPoint(32.0, 34.0))

        viewModel.onCitySelected(city)

        assertEquals("Tel Aviv", locationRepo.savedLabel)
        assertEquals(32.0, locationRepo.savedPoint?.latitudeDeg)
        assertTrue(viewModel.state.value.isLocationObtained)
    }

    @Test
    fun testOnUseLocationClicked_SavesLocationAndUpdatesState() = runBlocking {
        val deviceRepo = FakeDeviceLocationRepository()
        deviceRepo.location = GeoPoint(32.0, 34.0)
        val locationRepo = FakeLocationRepositoryForWelcome()
        val viewModel = WelcomeViewModel(FakeCitySearchRepository(), deviceRepo, locationRepo)

        viewModel.onUseLocationClicked()

        assertEquals("Current Location", locationRepo.savedLabel)
        assertEquals(32.0, locationRepo.savedPoint?.latitudeDeg)
        assertTrue(viewModel.state.value.isLocationObtained)
    }
}
