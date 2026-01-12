package day.presentation

import day.domain.GeoPoint
import day.domain.GetSunAngle
import day.domain.LocationChoice
import day.domain.LocationRepository
import day.domain.SavedLocation
import day.domain.SunEvents
import day.domain.SunRepository
import ui.theme.AppThemeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

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

class FakeSunRepositoryForDaySky(
    private val mockAngle: Float = 45f,
    private val mockEvents: SunEvents = SunEvents(
        sunrise = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        sunset = kotlinx.datetime.Instant.fromEpochMilliseconds(3600000)
    ),
    private val mockTomorrowEvents: SunEvents? = null
) : SunRepository {
    override suspend fun getSunAngleNowDeg(at: GeoPoint): Float {
        return mockAngle
    }

    override suspend fun getSunEvents(at: GeoPoint, date: String?): SunEvents {
        if (date != null && mockTomorrowEvents != null) return mockTomorrowEvents
        return mockEvents
    }
}

class FakeCloudRepositoryForDaySky : day.domain.CloudRepository {
    var mockResult: Result<day.domain.CloudResult> = Result.success(day.domain.CloudResult(emptyList(), day.domain.InputsUsed(0, null)))
    override suspend fun getCloudTypes(lat: Double, lon: Double): Result<day.domain.CloudResult> = mockResult
}

class DaySkyViewModelTest {
    @Test
    fun testRefresh_LoadsLocationAndSunAngle() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 75f)
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

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

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        assertEquals(120f, state.sunAngleDeg)
        assertEquals("Tokyo", state.location?.label)
        assertEquals(35.6762, state.location?.latitudeDeg)
        assertEquals(139.6503, state.location?.longitudeDeg)
    }

    @Test
    fun testOnSunTapped_SetsExpandedAndHidesInfoCard() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        // Wait for init
        delay(100)

        // Show info card first
        viewModel.onToggleInfoCard()
        assertEquals(true, viewModel.state.value.isInfoCardVisible)

        viewModel.onSunTapped()

        val state = viewModel.state.value
        assertEquals(true, state.isExpanded)
        assertEquals(false, state.isInfoCardVisible)
    }

    @Test
    fun testOnBackTapped_CollapsesExpanded() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

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

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

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
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        var navigated = false
        viewModel.onChangeLocationClicked {
            navigated = true
        }

        assertEquals(true, navigated)
        assertEquals("", locationRepo.mockLocation.label)
    }

    @Test
    fun testOnSunTapped_StartsCountdown_Daytime() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val now = Clock.System.now()
        val sunset = now + 3600.seconds
        val sunRepo = FakeSunRepositoryForDaySky(
            mockAngle = 90f,
            mockEvents = SunEvents(
                sunrise = now - 3600.seconds,
                sunset = sunset
            )
        )
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        viewModel.onSunTapped()
        delay(100) // wait for first tick

        val state = viewModel.state.value
        assertEquals(true, state.isBeforeSunset)
        assertEquals(false, state.isBeforeSunrise)
        assertTrue(state.countdownSeconds != null && state.countdownSeconds!! > 0)
        assertTrue(state.eventTime != null)
    }

    @Test
    fun testOnSunTapped_StartsCountdown_Nighttime_BeforeSunrise() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val now = Clock.System.now()
        val sunrise = now + 3600.seconds
        val sunRepo = FakeSunRepositoryForDaySky(
            mockAngle = 0f,
            mockEvents = SunEvents(
                sunrise = sunrise,
                sunset = now + 10000.seconds
            )
        )
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        viewModel.onSunTapped()
        delay(100) // wait for first tick

        val state = viewModel.state.value
        assertEquals(false, state.isBeforeSunset)
        assertEquals(true, state.isBeforeSunrise)
        assertTrue(state.countdownSeconds != null && state.countdownSeconds!! > 0)
        assertTrue(state.eventTime != null)
    }

    @Test
    fun testOnSunTapped_StartsCountdown_Nighttime_AfterSunset_BeforeTomorrowSunrise() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val now = Clock.System.now()
        // Today's sunrise and sunset are in the past
        val todaySunrise = now - 10000.seconds
        val todaySunset = now - 1000.seconds
        
        val tomorrowSunrise = now + 20000.seconds
        val tomorrowSunset = now + 60000.seconds

        val sunRepo = FakeSunRepositoryForDaySky(
            mockAngle = 180f, // Night
            mockEvents = SunEvents(
                sunrise = todaySunrise,
                sunset = todaySunset
            ),
            mockTomorrowEvents = SunEvents(
                sunrise = tomorrowSunrise,
                sunset = tomorrowSunset
            )
        )
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        viewModel.onSunTapped()
        delay(100) // wait for first tick

        val state = viewModel.state.value
        assertEquals(true, state.isBeforeSunrise, "Should be before (tomorrow's) sunrise even if today's sunrise is past")
        assertTrue(state.countdownSeconds != null, "Countdown should not be null")
        assertTrue(state.countdownSeconds!! > 0)
    }

    @Test
    fun testRefresh_Nighttime_AngleIs90() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 10f) // Nighttime
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        assertEquals(AppThemeType.NIGHT, state.themeType)
        assertEquals(90f, state.sunAngleDeg)
    }

    @Test
    fun testRefresh_Daytime_AngleIsActual() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky(mockAngle = 45f) // Daytime
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        // Wait for init to complete
        delay(100)

        val state = viewModel.state.value
        assertEquals(AppThemeType.DAY, state.themeType)
        assertEquals(45f, state.sunAngleDeg)
    }

    @Test
    fun testRefresh_LoadsClouds() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val cloudRepo = FakeCloudRepositoryForDaySky()
        cloudRepo.mockResult = Result.success(
            day.domain.CloudResult(
                listOf(day.domain.CloudType.CUMULUS),
                day.domain.InputsUsed(20, 800)
            )
        )

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, cloudRepo)

        delay(100)

        assertEquals(listOf(day.domain.CloudType.CUMULUS), viewModel.state.value.cloudTypes)
    }

    @Test
    fun testRefresh_NoClouds_ReturnsEmptyList() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val cloudRepo = FakeCloudRepositoryForDaySky()
        cloudRepo.mockResult = Result.success(
            day.domain.CloudResult(
                emptyList(),
                day.domain.InputsUsed(5, 800)
            )
        )

        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, cloudRepo)

        delay(100)

        assertEquals(emptyList(), viewModel.state.value.cloudTypes)
    }

    @Test
    fun testOnToggleInfoCard_ChangesVisibility() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        assertEquals(false, viewModel.state.value.isInfoCardVisible)

        viewModel.onToggleInfoCard()
        assertEquals(true, viewModel.state.value.isInfoCardVisible)

        viewModel.onToggleInfoCard()
        assertEquals(false, viewModel.state.value.isInfoCardVisible)
    }

    @Test
    fun testOnBackTapped_HidesInfoCardIfVisible() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        viewModel.onToggleInfoCard()
        assertEquals(true, viewModel.state.value.isInfoCardVisible)

        viewModel.onBackTapped()
        assertEquals(false, viewModel.state.value.isInfoCardVisible)
    }

    @Test
    fun testOnStarTapped_ShowsInfoCardWithStar() = runBlocking {
        val locationRepo = FakeLocationRepositoryForDaySky()
        val sunRepo = FakeSunRepositoryForDaySky()
        val getSunAngle = GetSunAngle(locationRepo, sunRepo)
        val viewModel = DaySkyViewModel(getSunAngle, locationRepo, sunRepo, FakeCloudRepositoryForDaySky())

        delay(100)
        viewModel.onStarTapped(day.domain.StarType.CAPELLA)
        
        val state = viewModel.state.value
        assertEquals(true, state.isInfoCardVisible)
        assertTrue(state.selectedInfo is InfoContent.Star)
        assertEquals(day.domain.StarType.CAPELLA, (state.selectedInfo as InfoContent.Star).type)
    }
}
