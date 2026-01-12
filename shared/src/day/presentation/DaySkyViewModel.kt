package day.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import day.domain.GetSunAngle
import day.domain.LocationRepository
import day.domain.SunRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ui.theme.AppThemeType
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

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
    val countdownSeconds: Long? = null,
    val eventTime: String? = null,
    val isBeforeSunset: Boolean = false,
    val isBeforeSunrise: Boolean = false,
)

class DaySkyViewModel(
    private val getSunAngleNow: GetSunAngle,
    private val locationRepo: LocationRepository,
    private val sunRepo: SunRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DaySkyState())
    val state: StateFlow<DaySkyState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var countdownJob: Job? = null

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
        startCountdown()
    }

    fun onBackTapped() {
        _state.update { it.copy(isExpanded = false) }
        stopCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000L)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _state.update { it.copy(countdownSeconds = null, eventTime = null, isBeforeSunset = false, isBeforeSunrise = false) }
    }

    private suspend fun updateCountdown() {
        val point = locationRepo.resolveActivePoint()
        val now = Clock.System.now()
        val themeType = _state.value.themeType
        val isNight = themeType == AppThemeType.NIGHT

        val events = try {
            val todayEvents = sunRepo.getSunEvents(point, null)
            if (!isNight) {
                if (now < todayEvents.sunset) {
                    todayEvents
                } else {
                    // It's day by theme but technically after sunset? Unlikely but handle it
                    null
                }
            } else {
                if (now < todayEvents.sunrise) {
                    todayEvents
                } else {
                    // Sunset has passed today, next sunrise is tomorrow
                    val tomorrow = (now + 24.hours).toLocalDateTime(TimeZone.currentSystemDefault())
                    val dateStr = "${tomorrow.year}-${tomorrow.monthNumber.toString().padStart(2, '0')}-${tomorrow.dayOfMonth.toString().padStart(2, '0')}"
                    sunRepo.getSunEvents(point, dateStr)
                }
            }
        } catch (e: Exception) {
            null
        }

        if (events == null) {
            _state.update { it.copy(countdownSeconds = null, eventTime = null, isBeforeSunset = false, isBeforeSunrise = false) }
            return
        }

        if (!isNight && now < events.sunset) {
            val diff = events.sunset.epochSeconds - now.epochSeconds
            val lt = events.sunset.toLocalDateTime(TimeZone.currentSystemDefault())
            _state.update {
                it.copy(
                    countdownSeconds = diff,
                    eventTime = "${lt.hour.toString().padStart(2, '0')}:${lt.minute.toString().padStart(2, '0')}",
                    isBeforeSunset = true,
                    isBeforeSunrise = false
                )
            }
        } else if (isNight && now < events.sunrise) {
            val diff = events.sunrise.epochSeconds - now.epochSeconds
            val lt = events.sunrise.toLocalDateTime(TimeZone.currentSystemDefault())
            _state.update {
                it.copy(
                    countdownSeconds = diff,
                    eventTime = "${lt.hour.toString().padStart(2, '0')}:${lt.minute.toString().padStart(2, '0')}",
                    isBeforeSunset = false,
                    isBeforeSunrise = true
                )
            }
        } else {
            _state.update { it.copy(countdownSeconds = null, eventTime = null, isBeforeSunset = false, isBeforeSunrise = false) }
        }
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
                val finalAngle = if (themeType == AppThemeType.NIGHT) 90f else angle
                _state.update {
                    it.copy(
                        sunAngleDeg = finalAngle,
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
        countdownJob?.cancel()
    }
}
