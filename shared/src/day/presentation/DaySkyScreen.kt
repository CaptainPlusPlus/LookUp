package day.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import ui.theme.LookUpTheme
import ui.theme.AppThemeType
import kotlin.math.cos
import kotlin.math.sin

sealed class DaySkyRoute(val route: String) {
    object BlueSky : DaySkyRoute("blue_sky")
    object GoldenHour : DaySkyRoute("golden_hour")
}

@Composable
private fun CountdownClock(
    seconds: Long,
    eventTime: String,
    isNight: Boolean,
    textColor: Color
) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val timeString = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                color = textColor,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                shadow = LookUpTheme.colors.textShadow
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isNight) "Sunrise at $eventTime" else "Sunset at $eventTime",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = textColor.copy(alpha = 0.8f),
                shadow = LookUpTheme.colors.textShadow
            )
        )
    }
}

@Composable
fun DaySkyScreenRoot(
    viewModel: DaySkyViewModel = koinViewModel(),
    onNavigateToWelcome: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isExpanded = currentRoute == DaySkyRoute.GoldenHour.route
    val themeType = if (isExpanded) AppThemeType.GOLDEN_HOUR else state.themeType

    LookUpTheme(themeType = themeType) {
        Box(modifier = Modifier.fillMaxSize()) {
            DaySkyScreen(
                state = state.copy(isExpanded = isExpanded),
                onSunClick = {
                    if (isExpanded) {
                        navController.popBackStack()
                        viewModel.onBackTapped()
                    } else {
                        navController.navigate(DaySkyRoute.GoldenHour.route)
                        viewModel.onSunTapped()
                    }
                },
                onChangeLocationClick = {
                    viewModel.onChangeLocationClicked(onNavigateToWelcome)
                }
            )

            NavHost(
                navController = navController,
                startDestination = DaySkyRoute.BlueSky.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(DaySkyRoute.BlueSky.route) {
                    // Future blue sky specific content
                }
                composable(DaySkyRoute.GoldenHour.route) {
                    // Future golden hour specific content
                }
            }

            BackHandler(enabled = isExpanded) {
                navController.popBackStack()
                viewModel.onBackTapped()
            }
        }
    }
}

@Composable
fun DaySkyScreen(
    state: DaySkyState,
    onSunClick: () -> Unit,
    onChangeLocationClick: () -> Unit
) {
    val themeColors = LookUpTheme.colors
    val animatedSkyTop by animateColorAsState(
        targetValue = themeColors.skyTop,
        animationSpec = tween(durationMillis = 600)
    )
    val animatedSkyBottom by animateColorAsState(
        targetValue = themeColors.skyBottom,
        animationSpec = tween(durationMillis = 600)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedSkyTop)
    ) {
        SunView(
            angleDeg = state.sunAngleDeg,
            color = themeColors.sunColor,
            isExpanded = state.isExpanded,
            isLoading = state.isLoading || state.location == null,
            themeType = LookUpTheme.themeType,
            onClick = onSunClick
        )
        
        // Cloud types at the center
        AnimatedVisibility(
            visible = !(state.isExpanded && state.countdownSeconds != null) && !state.isLoading,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500)) + slideOutVertically { it },
            modifier = Modifier.align(Alignment.Center)
        ) {
            val cloudText = if (state.cloudTypes.isEmpty()) {
                "NOTHING"
            } else {
                state.cloudTypes.joinToString(" & ") { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
            }
            Text(
                text = cloudText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = themeColors.textColor,
                    shadow = themeColors.textShadow
                )
            )
        }

        AnimatedVisibility(
            visible = state.isExpanded && state.countdownSeconds != null,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CountdownClock(
                seconds = state.countdownSeconds ?: 0,
                eventTime = state.eventTime ?: "",
                isNight = state.isBeforeSunrise,
                textColor = if (state.isBeforeSunrise) Color.Black else Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.location?.let {
                Text(
                    text = it.label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = themeColors.textColor,
                        shadow = themeColors.textShadow
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = onChangeLocationClick,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, themeColors.textColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Change Location",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = themeColors.textColor,
                            shadow = themeColors.textShadow
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SunView(
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
        }

        val sunCenter by animateOffsetAsState(
            targetValue = targetSunCenter,
            animationSpec = tween(durationMillis = 1000)
        )

        val sunCenterX = sunCenter.x
        val sunCenterY = sunCenter.y

        val lightRadius = if (themeType == AppThemeType.NIGHT) 233f else 700f

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
                        radius = lightRadius * density
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

    val targetSunbeamSize = if (isExpanded) sunSize * 20f else if (isLoading) sunSize * pulseScale else if (themeType == AppThemeType.NIGHT) sunSize * 0.5f else sunSize * 1.5f
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