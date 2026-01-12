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
        // Detect swipe from the left edge (first 20dp)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        if (dragAmount > 50) { // Simple threshold for back swipe
                            onBack()
                            change.consume()
                        }
                    }
                }
        )
    }
}
