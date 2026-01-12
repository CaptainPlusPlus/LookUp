//package ui.theme
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun ThemeComparisonPreview() {
//    Column(modifier = Modifier.fillMaxSize()) {
//        ThemeSample(AppThemeType.DAY, "Day Sky Theme")
//        ThemeSample(AppThemeType.GOLDEN_HOUR, "Golden Hour Theme")
//        ThemeSample(AppThemeType.NIGHT, "Night Sky Theme")
//    }
//}
//
//@Composable
//private fun ColumnScope.ThemeSample(type: AppThemeType, label: String) {
//    LookUpTheme(themeType = type) {
//        val colors = LookUpTheme.colors
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//                .background(
//                    brush = Brush.verticalGradient(
//                        colors = listOf(colors.skyTop, colors.skyBottom)
//                    )
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Text(
//                    text = "LookUp!",
//                    style = MaterialTheme.typography.displayLarge.copy(
//                        brush = Brush.linearGradient(
//                            colors = listOf(LookUpOrange, LookUpGray)
//                        ),
//                        shadow = colors.textShadow
//                    )
//                )
//                Text(
//                    text = label,
//                    style = MaterialTheme.typography.headlineMedium.copy(
//                        color = colors.textColor,
//                        shadow = colors.textShadow
//                    )
//                )
//            }
//        }
//    }
//}
