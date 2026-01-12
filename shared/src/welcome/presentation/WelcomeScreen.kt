package welcome.presentation


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import day.domain.CitySearchResult
import day.domain.GeoPoint
import ui.theme.LookUpTheme
import ui.theme.AppThemeType
import ui.theme.LookUpYellow
import ui.theme.LookUpWhite

import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shared.generated.resources.Res
import shared.generated.resources.pick_city
import shared.generated.resources.use_location
import shared.generated.resources.welcome_subtitle
import shared.generated.resources.welcome_title

// Use literal strings for now to avoid build issues with generated resources in this environment

@Composable
fun WelcomeScreenRoot(
    viewModel: WelcomeViewModel = koinViewModel(),
    onLocationObtained: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val locationRepository: day.domain.LocationRepository = org.koin.compose.koinInject()

    LaunchedEffect(Unit) {
        if (locationRepository.hasLocation()) {
            onLocationObtained()
        }
    }

    LaunchedEffect(state.isLocationObtained) {
        if (state.isLocationObtained) {
            onLocationObtained()
        }
    }

    WelcomeScreen(
        state = state,
        searchResults = searchResults,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCitySelected = viewModel::onCitySelected,
        onUseLocationClicked = viewModel::onUseLocationClicked
    )

    if (state.isSearching && state.isLocationObtained) {
        day.presentation.DaySkyScreen(
            state = day.presentation.DaySkyState(isLoading = true),
            onSunClick = {},
            onChangeLocationClick = {},
            onToggleInfoCard = {},
            onHideInfoCard = {},
            onStarClick = {}
        )
    }
}

@Composable
fun WelcomeScreen(
    state: WelcomeState,
    searchResults: List<CitySearchResult>,
    onSearchQueryChanged: (String) -> Unit,
    onCitySelected: (CitySearchResult) -> Unit,
    onUseLocationClicked: () -> Unit
) {
    LookUpTheme(themeType = AppThemeType.DAY) {
        val themeColors = LookUpTheme.colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            themeColors.skyTop,
                            themeColors.skyBottom
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 64.dp, vertical = 32.dp), // Even more padding to take up middle third
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.welcome_title),
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(LookUpYellow, LookUpWhite)
                        ),
                        shadow = themeColors.textShadow
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = themeColors.textColor,
                        shadow = themeColors.textShadow
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            label = { Text(stringResource(resource = Res.string.pick_city)) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = themeColors.textColor,
                                shadow = themeColors.textShadow
                            ),
                            trailingIcon = {
                                if (state.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = themeColors.textColor
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = themeColors.textColor,
                                unfocusedTextColor = themeColors.textColor,
                                focusedLabelColor = themeColors.accentColor,
                                unfocusedLabelColor = themeColors.textColor,
                                focusedBorderColor = themeColors.accentColor,
                                unfocusedBorderColor = themeColors.textColor.copy(alpha = 0.7f),
                                cursorColor = themeColors.accentColor
                            )
                        )

                        if (searchResults.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                tonalElevation = 8.dp,
                                color = Color.White.copy(alpha = 0.9f), // Whiter background for suggestions
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            ) {
                                LazyColumn {
                                    items(searchResults) { city ->
                                        ListItem(
                                            headlineContent = { 
                                                Text(
                                                    city.label,
                                                    color = Color.Black, // Black text for suggestions
                                                    style = TextStyle(shadow = null) // Remove shadow for clarity on white
                                                ) 
                                            },
                                            colors = ListItemDefaults.colors(
                                                containerColor = Color.Transparent
                                            ),
                                            modifier = Modifier.clickable { onCitySelected(city) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onUseLocationClicked,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.accentColor.takeOrElse { Color.DarkGray }
                    )
                ) {
                    Text(
                        stringResource(Res.string.use_location),
                        color = Color.White,
                        style = TextStyle(shadow = themeColors.textShadow)
                    )
                }

                state.error?.let {
                    Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(
        state = WelcomeState(
            searchQuery = "Tel Aviv",
            isSearching = false,
            error = null,
            isLocationObtained = false
        ),
        searchResults = listOf(
            CitySearchResult("1", "Tel Aviv, Israel", GeoPoint(32.0853, 34.7818)),
            CitySearchResult("2", "Tel Aviv District, Israel", GeoPoint(32.0853, 34.7818))
        ),
        onSearchQueryChanged = {},
        onCitySelected = {},
        onUseLocationClicked = {}
    )
}
