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

class DaySkyViewModel(
    private val getSunAngleNow: GetSunAngle,
    private val locationRepo: LocationRepository,
    private val sunRepo: SunRepository,
    private val cloudRepo: day.domain.CloudRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DaySkyState())
    val state: StateFlow<DaySkyState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var countdownJob: Job? = null

    init {
        refresh()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun onSunTapped() {
        _state.update { it.copy(isExpanded = true, isInfoCardVisible = false) }
        startCountdown()
    }

    fun onBackTapped() {
        if (_state.value.isInfoCardVisible) {
            _state.update { it.copy(isInfoCardVisible = false) }
            return
        }
        _state.update { it.copy(isExpanded = false) }
        stopCountdown()
    }

    fun onToggleInfoCard() {
        _state.update {
            val isVisible = !it.isInfoCardVisible
            it.copy(
                isInfoCardVisible = isVisible,
                selectedInfo = if (isVisible) it.cloudTypes.firstOrNull()?.let { type -> InfoContent.Cloud(type) } else it.selectedInfo
            )
        }
    }

    fun onHideInfoCard() {
        _state.update { it.copy(isInfoCardVisible = false) }
    }

    fun onStarTapped(starType: day.domain.StarType) {
        _state.update { it.copy(isInfoCardVisible = true, selectedInfo = InfoContent.Star(starType)) }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(COUNTDOWN_DELAY_MS)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _state.update {
            it.copy(
                countdownSeconds = null,
                eventTime = null,
                isBeforeSunset = false,
                isBeforeSunrise = false
            )
        }
    }

    private suspend fun updateCountdown() {
        val point = locationRepo.resolveActivePoint()
        val now = Clock.System.now()
        val themeType = _state.value.themeType
        val isNight = themeType == AppThemeType.NIGHT

        val events = try {
            val todayEvents = sunRepo.getSunEvents(point, null)
            if (!isNight) {
                if (now < todayEvents.sunset) todayEvents else null
            } else {
                if (now < todayEvents.sunrise) {
                    todayEvents
                } else {
                    val tomorrow = (now + 24.hours).toLocalDateTime(TimeZone.currentSystemDefault())
                    val dateStr = formatLocalDate(tomorrow)
                    sunRepo.getSunEvents(point, dateStr)
                }
            }
        } catch (e: Exception) {
            null
        }

        if (events == null) {
            clearCountdownState()
            return
        }

        if (!isNight && now < events.sunset) {
            val diff = events.sunset.epochSeconds - now.epochSeconds
            val lt = events.sunset.toLocalDateTime(TimeZone.currentSystemDefault())
            _state.update {
                it.copy(
                    countdownSeconds = diff,
                    eventTime = formatTime(lt.hour, lt.minute),
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
                    eventTime = formatTime(lt.hour, lt.minute),
                    isBeforeSunset = false,
                    isBeforeSunrise = true
                )
            }
        } else {
            clearCountdownState()
        }
    }

    private fun clearCountdownState() {
        _state.update {
            it.copy(
                countdownSeconds = null,
                eventTime = null,
                isBeforeSunset = false,
                isBeforeSunrise = false
            )
        }
    }

    private fun formatLocalDate(date: kotlinx.datetime.LocalDateTime): String {
        val year = date.year
        val month = date.monthNumber.toString().padStart(2, '0')
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
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
                val clouds = cloudRepo.getCloudTypes(point.latitudeDeg, point.longitudeDeg)
                    .getOrDefault(
                        day.domain.CloudResult(
                            emptyList(),
                            day.domain.InputsUsed(0, null)
                        )
                    )
                Triple(UiLocation(label, point.latitudeDeg, point.longitudeDeg), angle, clouds.types)
            }.onSuccess { (uiLocation, angle, cloudTypes) ->
                val themeType = determineThemeType(angle)
                val finalAngle = if (themeType == AppThemeType.NIGHT) NIGHT_SUN_ANGLE else angle
                _state.update {
                    it.copy(
                        sunAngleDeg = finalAngle,
                        location = uiLocation,
                        isLoading = false,
                        error = null,
                        themeType = themeType,
                        cloudTypes = cloudTypes,
                        selectedInfo = it.selectedInfo ?: cloudTypes.firstOrNull()?.let { type -> InfoContent.Cloud(type) }
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

    private fun determineThemeType(angle: Float): AppThemeType {
        return when {
            angle < NIGHT_THRESHOLD || angle > (180f - NIGHT_THRESHOLD) -> AppThemeType.NIGHT
            (angle in NIGHT_THRESHOLD..GOLDEN_HOUR_THRESHOLD) || (angle in (180f - GOLDEN_HOUR_THRESHOLD)..(180f - NIGHT_THRESHOLD)) -> AppThemeType.GOLDEN_HOUR
            else -> AppThemeType.DAY
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        countdownJob?.cancel()
    }

    companion object {
        private const val TICK_INTERVAL_MS = 60_000L
        private const val COUNTDOWN_DELAY_MS = 1000L
        private const val NIGHT_THRESHOLD = 15f
        private const val GOLDEN_HOUR_THRESHOLD = 35f
        private const val NIGHT_SUN_ANGLE = 90f
    }
}
