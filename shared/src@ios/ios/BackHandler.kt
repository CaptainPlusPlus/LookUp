package day.presentation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    if (enabled) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(EDGE_SWIPE_WIDTH.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        if (dragAmount > SWIPE_THRESHOLD) {
                            onBack()
                            change.consume()
                        }
                    }
                }
        )
    }
}

private const val EDGE_SWIPE_WIDTH = 20
private const val SWIPE_THRESHOLD = 50
