package welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import day.domain.CitySearchRepository
import day.domain.CitySearchResult
import day.domain.DeviceLocationRepository
import day.domain.LocationRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WelcomeState(
    val searchQuery: String = "",
    val searchResults: List<CitySearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val isLocationObtained: Boolean = false
)

class WelcomeViewModel(
    private val citySearchRepo: CitySearchRepository,
    private val deviceLocationRepo: DeviceLocationRepository,
    private val locationRepo: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeState())
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults = _state
        .map { it.searchQuery }
        .distinctUntilChanged()
        .debounce(SEARCH_DEBOUNCE_MS)
        .mapLatest { query ->
            if (query.isBlank()) emptyList()
            else {
                _state.update { it.copy(isSearching = true) }
                try {
                    val results = citySearchRepo.searchCity(query)
                    _state.update { it.copy(isSearching = false) }
                    results
                } catch (e: Exception) {
                    _state.update { it.copy(isSearching = false, error = e.message) }
                    emptyList()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCitySelected(city: CitySearchResult) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(LOADING_DELAY_MS)
            locationRepo.saveLocation(
                label = city.label,
                point = city.point,
                isCurrent = true
            )
            _state.update { it.copy(isLocationObtained = true, isSearching = false) }
        }
    }

    fun onUseLocationClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }
            val location = deviceLocationRepo.getCurrentLocation()
            delay(LOADING_DELAY_MS)
            if (location != null) {
                locationRepo.saveLocation(
                    label = DEFAULT_LOCATION_LABEL,
                    point = location,
                    isCurrent = true
                )
                _state.update { it.copy(isLocationObtained = true, isSearching = false) }
            } else {
                _state.update { it.copy(error = ERROR_DEVICE_LOCATION, isSearching = false) }
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val LOADING_DELAY_MS = 500L
        private const val DEFAULT_LOCATION_LABEL = "Current Location"
        private const val ERROR_DEVICE_LOCATION = "Could not get device location"
    }
}
