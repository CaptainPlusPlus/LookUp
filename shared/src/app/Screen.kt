package app
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import day.presentation.DaySkyScreenRoot

@Composable
fun Screen() {
    MaterialTheme {
        DaySkyScreenRoot(
            onSunClick = {
                // Handle sun click if needed
            }
        )
    }
}
