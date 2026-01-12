package day.presentation

import ui.theme.AppThemeType

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
    val cloudTypes: List<day.domain.CloudType> = emptyList(),
    val isInfoCardVisible: Boolean = false,
    val selectedInfo: InfoContent? = null,
)

sealed interface InfoContent {
    data class Cloud(val type: day.domain.CloudType) : InfoContent
    data class Star(val type: day.domain.StarType) : InfoContent
}
