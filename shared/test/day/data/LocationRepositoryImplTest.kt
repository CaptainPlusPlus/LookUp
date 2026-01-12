package day.data

import day.data.storage.LocationEntity
import day.data.storage.PreferencesStorage
import day.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakePreferencesStorage : PreferencesStorage {
    private var currentLocation: LocationEntity? = null

    override suspend fun getCurrentLocation(): LocationEntity? {
        return currentLocation
    }

    override suspend fun saveCurrentLocation(location: LocationEntity) {
        currentLocation = location.copy(isCurrent = true)
    }

    override suspend fun clearCurrentLocation() {
        currentLocation = null
    }

    override suspend fun getAllLocations(): List<LocationEntity> {
        return listOfNotNull(currentLocation)
    }
}

class LocationRepositoryImplTest {

    @Test
    fun testHasLocation_ReturnsTrueWhenLocationExists() = runBlocking {
        val storage = FakePreferencesStorage()
        storage.saveCurrentLocation(
            LocationEntity(
                id = "test",
                label = "Test City",
                latitudeDeg = 51.5,
                longitudeDeg = -0.1,
                isCurrent = true
            )
        )

        val repository = LocationRepositoryImpl(storage)
        val result = repository.hasLocation()

        assertTrue(result)
    }

    @Test
    fun testHasLocation_ReturnsFalseWhenNoLocation() = runBlocking {
        val storage = FakePreferencesStorage()

        val repository = LocationRepositoryImpl(storage)
        val result = repository.hasLocation()

        assertFalse(result)
    }

    @Test
    fun testSaveLocation_CallsStorageSave() = runBlocking {
        val storage = FakePreferencesStorage()
        val repository = LocationRepositoryImpl(storage)

        val label = "Test City"
        val point = GeoPoint(51.5, -0.1)

        repository.saveLocation(label, point, isCurrent = true)

        val current = storage.getCurrentLocation()
        assertEquals(label, current?.label)
        assertEquals(51.5, current?.latitudeDeg)
        assertEquals(-0.1, current?.longitudeDeg)
        assertTrue(current?.isCurrent == true)
    }

    @Test
    fun testResolveActivePoint_ReturnsPointFromStorage() = runBlocking {
        val storage = FakePreferencesStorage()
        storage.saveCurrentLocation(
            LocationEntity(
                id = "test",
                label = "Test City",
                latitudeDeg = 51.5,
                longitudeDeg = -0.1,
                isCurrent = true
            )
        )

        val repository = LocationRepositoryImpl(storage)
        val result = repository.resolveActivePoint()

        assertEquals(51.5, result.latitudeDeg)
        assertEquals(-0.1, result.longitudeDeg)
    }

    @Test
    fun testGetActiveLabel_ReturnsLabelFromStorage() = runBlocking {
        val storage = FakePreferencesStorage()
        storage.saveCurrentLocation(
            LocationEntity(
                id = "test",
                label = "Test City",
                latitudeDeg = 51.5,
                longitudeDeg = -0.1,
                isCurrent = true
            )
        )

        val repository = LocationRepositoryImpl(storage)
        val result = repository.getActiveLabel()

        assertEquals("Test City", result)
    }

    @Test
    fun testSaveLocation_WithIsCurrentFalse_DoesNotStore() = runBlocking {
        val storage = FakePreferencesStorage()

        // Save a current location first
        storage.saveCurrentLocation(
            LocationEntity(
                id = "first",
                label = "First City",
                latitudeDeg = 51.5,
                longitudeDeg = -0.1,
                isCurrent = true
            )
        )

        val repository = LocationRepositoryImpl(storage)

        // Save second location as non-current
        repository.saveLocation("Second City", GeoPoint(40.7, -74.0), isCurrent = false)

        // First should still be current
        val current = storage.getCurrentLocation()
        assertEquals("First City", current?.label)
    }

    @Test
    fun testClearLocation_ClearsCurrentInStorage() = runBlocking {
        val storage = FakePreferencesStorage()
        storage.saveCurrentLocation(
            LocationEntity("id", "Label", 1.0, 2.0, true)
        )
        val repository = LocationRepositoryImpl(storage)
        
        repository.clearLocation()
        
        assertFalse(repository.hasLocation())
        assertEquals(null, storage.getCurrentLocation())
    }
}
