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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.Dp
import shared.generated.resources.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import day.domain.CloudType
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
            text = if (isNight) stringResource(Res.string.sunrise_at, eventTime) else stringResource(Res.string.sunset_at, eventTime),
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
                },
                onToggleInfoCard = viewModel::onToggleInfoCard,
                onHideInfoCard = viewModel::onHideInfoCard
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

            BackHandler(enabled = isExpanded || state.isInfoCardVisible) {
                if (state.isInfoCardVisible) {
                    viewModel.onHideInfoCard()
                } else if (isExpanded) {
                    navController.popBackStack()
                    viewModel.onBackTapped()
                }
            }
        }
    }
}

@Composable
private fun CloudView(cloudTypes: List<CloudType>) {
    val cloudType = cloudTypes.firstOrNull() ?: return
    
    val infiniteTransition = rememberInfiniteTransition(label = "cloudOffset")
    val xOffset by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 0.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.dp at 0 with LinearEasing
                (-4).dp at 500 with LinearEasing
                (-4).dp at 1000 with LinearEasing
                0.dp at 1500 with LinearEasing
                0.dp at 2000 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "xOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .offset(x = xOffset),
        contentAlignment = Alignment.Center
    ) {
        val resource = when (cloudType) {
            CloudType.CUMULUS -> Res.drawable.CUMULUS
            CloudType.STRATUS -> Res.drawable.STRATUS
            CloudType.CIRRUS -> Res.drawable.CIRRUS
            CloudType.NIMBUS -> Res.drawable.NIMBUS
        }
        Image(
            painter = painterResource(resource),
            contentDescription = cloudType.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f) // Adjust aspect ratio as needed to match your webp images
        )
    }
}

@Composable
fun DaySkyScreen(
    state: DaySkyState,
    onSunClick: () -> Unit,
    onChangeLocationClick: () -> Unit,
    onToggleInfoCard: () -> Unit,
    onHideInfoCard: () -> Unit
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedSkyTop)
    ) {
        val screenHeight = maxHeight
        val cardHeight = screenHeight * 0.7f
        
        val infoCardOffset by animateDpAsState(
            targetValue = if (state.isInfoCardVisible) 32.dp else cardHeight,
            animationSpec = if (state.isExpanded) tween(0) else tween(durationMillis = 500)
        )

        val lastCountdownSeconds = remember { mutableStateOf<Long?>(null) }
        val lastEventTime = remember { mutableStateOf<String?>(null) }
        val lastIsBeforeSunrise = remember { mutableStateOf(false) }

        if (state.countdownSeconds != null) {
            lastCountdownSeconds.value = state.countdownSeconds
            lastEventTime.value = state.eventTime
            lastIsBeforeSunrise.value = state.isBeforeSunrise
        }

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
            if (state.cloudTypes.isNotEmpty()) {
                CloudView(cloudTypes = state.cloudTypes)
            }
        }

        AnimatedVisibility(
            visible = state.isExpanded && state.countdownSeconds != null,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CountdownClock(
                seconds = lastCountdownSeconds.value ?: 0,
                eventTime = lastEventTime.value ?: "",
                isNight = lastIsBeforeSunrise.value,
                textColor = if (lastIsBeforeSunrise.value) Color.Black else Color.White
            )
        }

        // Location info
        AnimatedVisibility(
            visible = !state.isInfoCardVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 120.dp),
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
                            text = stringResource(Res.string.change_location),
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

        // Info Button and Card
        val showInfoButton = !(state.isExpanded && state.countdownSeconds != null) && !state.isLoading && state.cloudTypes.isNotEmpty()
        
        if (showInfoButton || state.isInfoCardVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = infoCardOffset)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Button
                Surface(
                    onClick = onToggleInfoCard,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(56.dp),
                    shape = CircleShape,
                    color = themeColors.skyBottom.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.isInfoCardVisible) "✕" else "i",
                            style = MaterialTheme.typography.titleLarge.copy(color = themeColors.textColor)
                        )
                    }
                }
                
                // The Card
                if (state.cloudTypes.isNotEmpty()) {
                    InfoCard(
                        cloudType = state.cloudTypes.first(),
                        onClose = onHideInfoCard,
                        modifier = Modifier.height(cardHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    cloudType: CloudType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LookUpTheme.colors
    val description = when (cloudType) {
        CloudType.CUMULUS -> stringResource(Res.string.cloud_cumulus_desc)
        CloudType.STRATUS -> stringResource(Res.string.cloud_stratus_desc)
        CloudType.CIRRUS -> stringResource(Res.string.cloud_cirrus_desc)
        CloudType.NIMBUS -> stringResource(Res.string.cloud_nimbus_desc)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20) { // Sliding down
                        onClose()
                    }
                }
            },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = themeColors.skyBottom.copy(alpha = 1.0f),
        contentColor = themeColors.textColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(themeColors.textColor.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = cloudType.name,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    shadow = themeColors.textShadow
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = themeColors.textShadow
                    )
                )
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