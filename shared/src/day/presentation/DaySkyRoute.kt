package day.presentation

sealed class DaySkyRoute(val route: String) {
    data object BlueSky : DaySkyRoute("blue_sky")
    data object GoldenHour : DaySkyRoute("golden_hour")
}
