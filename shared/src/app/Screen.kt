package app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import day.presentation.DaySkyScreenRoot

import ui.theme.LookUpTheme
import ui.theme.AppThemeType

@Composable
fun Screen() {
    LookUpTheme(themeType = AppThemeType.DAY) {
        AppNavigation()
    }
}
