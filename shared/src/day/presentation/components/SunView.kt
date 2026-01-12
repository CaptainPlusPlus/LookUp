package day.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ui.theme.AppThemeType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunView(
    angleDeg: Float,
    color: Color,
    isExpanded: Boolean,
    isLoading: Boolean,
    themeType: AppThemeType,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        val density = LocalDensity.current.density

        val targetSunCenter by remember(width, height, angleDeg, isLoading) {
            derivedStateOf {
                if (isLoading) {
                    Offset(width.value / 2f, height.value / 2f)
                } else {
                    val verticalBound = height * 0.4f
                    val radiusX = width / 2f
                    val radiusY = verticalBound * 0.8f
                    val angleRad = (angleDeg * kotlin.math.PI / 180.0).toFloat()

                    val x = (width.value / 2f) + (radiusX.value * cos(angleRad))
                    val y = (verticalBound.value) - (radiusY.value * sin(angleRad))
                    Offset(x, y)
                }
            }
        }

        val sunCenter by animateOffsetAsState(
            targetValue = targetSunCenter,
            animationSpec = tween(durationMillis = 1000)
        )

        val lightRadius = if (themeType == AppThemeType.NIGHT) 233f else 700f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .fillMaxHeight(0.66f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        center = Offset(sunCenter.x * density, sunCenter.y * density),
                        radius = lightRadius * density
                    )
                )
        )

        SunBody(
            centerX = sunCenter.x,
            centerY = sunCenter.y,
            color = color,
            density = density,
            isExpanded = isExpanded,
            isLoading = isLoading,
            themeType = themeType,
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
    isLoading: Boolean,
    themeType: AppThemeType,
    onClick: () -> Unit
) {
    val sunSize = 90.dp
    
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val targetSunbeamSize = when {
        isExpanded -> sunSize * 20f
        isLoading -> sunSize * pulseScale
        themeType == AppThemeType.NIGHT -> sunSize * 1.2f
        else -> sunSize * 1.5f
    }
    
    val sunbeamSize by animateDpAsState(
        targetValue = targetSunbeamSize,
        animationSpec = if (isLoading) tween(0) else tween(durationMillis = 1000)
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
                    indication = null,
                    enabled = !isLoading
                ) { onClick() }
        )
    }
}
