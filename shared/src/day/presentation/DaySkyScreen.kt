package day.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import day.domain.StarType
import day.presentation.components.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shared.generated.resources.*
import ui.theme.AppThemeType
import ui.theme.LookUpTheme

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
                onHideInfoCard = viewModel::onHideInfoCard,
                onStarClick = viewModel::onStarTapped
            )

            NavHost(
                navController = navController,
                startDestination = DaySkyRoute.BlueSky.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(DaySkyRoute.BlueSky.route) {}
                composable(DaySkyRoute.GoldenHour.route) {}
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
fun DaySkyScreen(
    state: DaySkyState,
    onSunClick: () -> Unit,
    onChangeLocationClick: () -> Unit,
    onToggleInfoCard: () -> Unit,
    onHideInfoCard: () -> Unit,
    onStarClick: (StarType) -> Unit
) {
    val themeColors = LookUpTheme.colors
    val animatedSkyTop by animateColorAsState(
        targetValue = themeColors.skyTop,
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

        val hoverTransition = rememberInfiniteTransition(label = "cloudHover")
        val hoverOffset by hoverTransition.animateValue(
            initialValue = 0.dp,
            targetValue = 0.dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.dp at 0 with LinearEasing
                    (-2).dp at 250 with LinearEasing
                    (-4).dp at 500 with LinearEasing
                    (-2).dp at 750 with LinearEasing
                    0.dp at 1000 with LinearEasing
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "hoverOffset"
        )

        val lastCountdownSeconds = remember { mutableStateOf<Long?>(null) }
        val lastEventTime = remember { mutableStateOf<String?>(null) }
        val lastIsBeforeSunrise = remember { mutableStateOf(false) }

        if (state.countdownSeconds != null) {
            lastCountdownSeconds.value = state.countdownSeconds
            lastEventTime.value = state.eventTime
            lastIsBeforeSunrise.value = state.isBeforeSunrise
        }

        val centerOffset by animateDpAsState(
            targetValue = if (state.isExpanded) (-150).dp else 0.dp,
            animationSpec = tween(durationMillis = 1000)
        )

        val cloudScale = if (LookUpTheme.themeType == AppThemeType.DAY) 4f else 2.5f

        SunView(
            angleDeg = state.sunAngleDeg,
            color = themeColors.sunColor,
            isExpanded = state.isExpanded,
            isLoading = state.isLoading || state.location == null,
            themeType = LookUpTheme.themeType,
            onClick = onSunClick
        )
        
        AnimatedVisibility(
            visible = !(state.isExpanded && state.countdownSeconds != null) && !state.isLoading,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500)) + slideOutVertically { it },
            modifier = Modifier.align(Alignment.Center).offset(y = centerOffset)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (LookUpTheme.themeType == AppThemeType.NIGHT) {
                        StarView(StarType.CAPELLA, onClick = { onStarClick(StarType.CAPELLA) })
                        StarView(StarType.CASTOR, onClick = { onStarClick(StarType.CASTOR) })
                    }
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (state.cloudTypes.isNotEmpty()) {
                        CloudView(
                            cloudTypes = state.cloudTypes,
                            scale = cloudScale
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (LookUpTheme.themeType == AppThemeType.NIGHT) {
                        StarView(StarType.SIRIUS, onClick = { onStarClick(StarType.SIRIUS) })
                        StarView(StarType.RIGEL, onClick = { onStarClick(StarType.RIGEL) })
                    }
                }
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
                textColor = if (lastIsBeforeSunrise.value) Color.White else Color.Black
            )
        }

        AnimatedVisibility(
            visible = !state.isInfoCardVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 120.dp),
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
                Image(
                    painter = painterResource(Res.drawable.CLOUD),
                    contentDescription = "Toggle Info",
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(84.dp)
                        .offset(y = hoverOffset)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleInfoCard
                        )
                )
                
                state.selectedInfo?.let { info ->
                    InfoCard(
                        content = info,
                        onClose = onHideInfoCard,
                        modifier = Modifier.height(cardHeight)
                    )
                }
            }
        }
    }
}