package day.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
fun DaySkyScreenRoot(
    viewModel: DaySkyViewModel = koinViewModel(),
    onNavigateToWelcome: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isExpanded = currentRoute == DaySkyRoute.GoldenHour.route

    LookUpTheme(themeType = state.themeType) {
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
    val skyColor by animateColorAsState(
        targetValue = themeColors.skyBottom, // Use bottom color as representative or keep logic? 
        // Actually, we should probably use a gradient here too.
        animationSpec = tween(durationMillis = 600)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(themeColors.skyTop, themeColors.skyBottom)
                )
            )
    ) {
        SunView(
            angleDeg = state.sunAngleDeg,
            color = themeColors.sunColor,
            isExpanded = state.isExpanded,
            onClick = onSunClick
        )

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