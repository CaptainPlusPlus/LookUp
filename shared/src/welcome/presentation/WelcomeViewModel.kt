package welcome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import day.domain.CitySearchRepository
import day.domain.CitySearchResult
import day.domain.DeviceLocationRepository
import day.domain.LocationRepository
import kotlinx.coroutines.FlowPreview
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
        .debounce(500)
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
            locationRepo.saveLocation(
                label = city.label,
                point = city.point,
                isCurrent = true
            )
            _state.update { it.copy(isLocationObtained = true) }
        }
    }

    fun onUseLocationClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }
            val location = deviceLocationRepo.getCurrentLocation()
            if (location != null) {
                locationRepo.saveLocation(
                    label = "Current Location",
                    point = location,
                    isCurrent = true
                )
                _state.update { it.copy(isLocationObtained = true, isSearching = false) }
            } else {
                _state.update { it.copy(error = "Could not get device location", isSearching = false) }
            }
        }
    }
}
