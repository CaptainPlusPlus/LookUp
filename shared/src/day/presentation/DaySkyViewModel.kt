package day.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import day.domain.GetSunAngle
import day.domain.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ui.theme.AppThemeType
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiLocation(
    val label: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
)

data class DaySkyState(
    val sunAngleDeg: Float = 0f,
    val location: UiLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isExpanded: Boolean = false,
    val themeType: AppThemeType = AppThemeType.DAY,
)

class DaySkyViewModel(
    private val getSunAngleNow: GetSunAngle,
    private val locationRepo: LocationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DaySkyState())
    val state: StateFlow<DaySkyState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    init {
        refresh()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(60_000L)
                refresh()
            }
        }
    }

    fun onSunTapped() {
        _state.update { it.copy(isExpanded = true) }
    }

    fun onBackTapped() {
        _state.update { it.copy(isExpanded = false) }
    }

    fun onChangeLocationClicked(onNavToWelcome: () -> Unit) {
        viewModelScope.launch {
            locationRepo.clearLocation()
            onNavToWelcome()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                val label = locationRepo.getActiveLabel()
                val point = locationRepo.resolveActivePoint()
                val angle = getSunAngleNow()
                UiLocation(label, point.latitudeDeg, point.longitudeDeg) to angle
            }.onSuccess { (uiLocation, angle) ->
                val themeType = when {
                    angle < 15f || angle > 165f -> AppThemeType.NIGHT
                    (angle in 15f..35f) || (angle in 145f..165f) -> AppThemeType.GOLDEN_HOUR
                    else -> AppThemeType.DAY
                }
                _state.update {
                    it.copy(
                        sunAngleDeg = angle,
                        location = uiLocation,
                        isLoading = false,
                        error = null,
                        themeType = themeType
                    )
                }
            }.onFailure { t ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
