package day.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shared.generated.resources.Res
import shared.generated.resources.sunrise_at
import shared.generated.resources.sunset_at
import ui.theme.LookUpTheme

@Composable
fun CountdownClock(
    seconds: Long,
    eventTime: String,
    isNight: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val timeString = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                color = textColor,
                fontFamily = FontFamily.Monospace,
                shadow = LookUpTheme.colors.textShadow
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isNight) {
                stringResource(Res.string.sunrise_at, eventTime)
            } else {
                stringResource(Res.string.sunset_at, eventTime)
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                color = textColor.copy(alpha = 0.8f),
                shadow = LookUpTheme.colors.textShadow
            )
        )
    }
}
