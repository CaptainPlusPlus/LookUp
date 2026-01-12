import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.Screen
import di.initKoin

fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication) {
            Screen()
        }
    }
}
