package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset

@Immutable
data class AppColors(
    val skyTop: Color,
    val skyBottom: Color,
    val sunColor: Color,
    val textColor: Color,
    val textShadow: Shadow?,
    val accentColor: Color = Color.Unspecified
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        skyTop = Color.Unspecified,
        skyBottom = Color.Unspecified,
        sunColor = Color.Unspecified,
        textColor = Color.Unspecified,
        textShadow = null
    )
}

enum class AppThemeType {
    DAY, GOLDEN_HOUR, NIGHT
}

@Composable
fun LookUpTheme(
    themeType: AppThemeType = AppThemeType.DAY,
    content: @Composable () -> Unit
) {
    val appColors = when (themeType) {
        AppThemeType.DAY -> AppColors(
            skyTop = DaySkyTop,
            skyBottom = DaySkyBottom,
            sunColor = SunYellow,
            textColor = Color.White,
            textShadow = Shadow(
                color = DayTextShadow,
                offset = Offset(2f, 2f),
                blurRadius = 4f
            ),
            accentColor = Color(0xFF4682B4) // SteelBlue for accent in Day theme
        )
        AppThemeType.GOLDEN_HOUR -> AppColors(
            skyTop = GoldenSkyTop,
            skyBottom = GoldenSkyBottom,
            sunColor = SunYellow,
            textColor = Color.Black,
            textShadow = Shadow(
                color = GoldenTextShadow,
                offset = Offset(2f, 2f),
                blurRadius = 4f
            ),
            accentColor = GoldenAccentRed
        )
        AppThemeType.NIGHT -> AppColors(
            skyTop = NightSkyTop,
            skyBottom = NightSkyBottom,
            sunColor = Color.Transparent, // No sun at night? Or moon?
            textColor = NightText,
            textShadow = null
        )
    }

    val colorScheme = when (themeType) {
        AppThemeType.NIGHT -> darkColorScheme()
        else -> lightColorScheme()
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

object LookUpTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}
