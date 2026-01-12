import androidx.compose.ui.window.ComposeUIViewController
import app.Screen
import di.initKoin

private var koinInitialized = false

fun ViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        initKoin()
        koinInitialized = true
    }
    Screen()
}
