package day.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DaySkyScreenRoot(
    viewModel: DaySkyViewModel = koinViewModel(),
    onSunClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        DaySkyScreen(
            state = state,
            onSunClick = {
                if (state.isExpanded) {
                    viewModel.onBackTapped()
                } else {
                    viewModel.onSunTapped()
                    onSunClick()
                }
            },
        )

        BackHandler(enabled = state.isExpanded) {
            viewModel.onBackTapped()
        }
    }
}

@Composable
fun DaySkyScreen(
    state: DaySkyState,
    onSunClick: () -> Unit,
) {
    val skyColor by animateColorAsState(
        targetValue = if (state.isExpanded) Color(0xFFFF7077) else Color(0xFF87CEEB), // Golden hour pink vs Teal/Light Blue
        animationSpec = tween(durationMillis = 600)
    )
    val sunColor = Color(0xFFFFA500)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyColor)
    ) {
        val mockSunAngle = 120f
        
        SunView(
            angleDeg = mockSunAngle,
            color = sunColor,
            isExpanded = state.isExpanded,
            onClick = onSunClick
        )
    }
}

@Composable
private fun SunView(
    angleDeg: Float,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        val density = LocalDensity.current.density

        val sunCenter by remember(width, height, angleDeg, density) {
            derivedStateOf {
                val verticalBound = height * (2f / 5f)
                val radiusX = width / 2f
                val radiusY = verticalBound * 0.8f

                val angleRad = (angleDeg * kotlin.math.PI / 180.0).toFloat()

                val xOffsetFromCenter = (radiusX.value * cos(angleRad))
                val yOffsetFromCenter = (radiusY.value * sin(angleRad))

                val x = (width.value / 2f) + xOffsetFromCenter
                val y = (verticalBound.value) - yOffsetFromCenter
                Offset(x, y)
            }
        }

        val sunCenterX = sunCenter.x
        val sunCenterY = sunCenter.y

        // Light effect centered on sun using radial gradient background.
        // Constrained to the top 2/3rds of the screen height.
        Box(
            modifier = Modifier
                .fillMaxSize(fraction = 1f)
                .align(Alignment.TopCenter)
                .fillMaxHeight(2f / 3f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        center = Offset(
                            x = sunCenterX * density,
                            y = sunCenterY * density
                        ),
                        radius = 700f * density // 700.dp radius
                    )
                )
        )

        // Sun Body
        SunBody(
            centerX = sunCenterX,
            centerY = sunCenterY,
            color = color,
            density = density,
            isExpanded = isExpanded,
            onClick = onClick
        )
    }
}

@Composable
private fun SunBody(
    centerX: Float,
    centerY: Float,
    color: Color,
    density: Float,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val sunSize = 90.dp
    val targetSunbeamSize = if (isExpanded) sunSize * 20f else sunSize * 1.5f
    val sunbeamSize by animateDpAsState(
        targetValue = targetSunbeamSize,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val center = Offset(centerX.dp.toPx(), centerY.dp.toPx())
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = (sunbeamSize.value * density / 2f),
                    center = center
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = centerX.dp - (sunSize / 2f),
                    y = centerY.dp - (sunSize / 2f)
                )
                .size(sunSize)
                .background(
                    brush = Brush.verticalGradient(colors = listOf(Color.White, color)),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
        )
    }
}