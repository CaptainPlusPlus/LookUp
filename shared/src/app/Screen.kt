package app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import day.presentation.DaySkyScreenRoot
import org.koin.compose.viewmodel.koinViewModel
import welcome.presentation.WelcomeScreenRoot

@Composable
fun Screen(
    viewModel: RootViewModel = koinViewModel()
) {
    MaterialTheme {
        val state by viewModel.state.collectAsState()

        when {
            state.hasLocation == null -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                AnimatedContent(
                    targetState = state.showWelcome,
                    transitionSpec = {
                        // Slide up from bottom when transitioning from Welcome to DaySky
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(600)
                        ) togetherWith slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(600)
                        )
                    },
                    label = "screen_transition"
                ) { showWelcome ->
                    if (showWelcome) {
                        WelcomeScreenRoot(
                            onLocationObtained = viewModel::onLocationObtained
                        )
                    } else {
                        DaySkyScreenRoot()
                    }
                }
            }
        }
    }
}
