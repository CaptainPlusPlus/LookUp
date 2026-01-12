package day.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import day.domain.CloudType
import day.domain.StarType
import day.presentation.InfoContent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shared.generated.resources.*
import ui.theme.LookUpTheme

@Composable
fun InfoCard(
    content: InfoContent,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LookUpTheme.colors
    val (title, description, resource) = when (content) {
        is InfoContent.Cloud -> {
            val desc = when (content.type) {
                CloudType.CUMULUS -> stringResource(Res.string.cloud_cumulus_desc)
                CloudType.STRATUS -> stringResource(Res.string.cloud_stratus_desc)
                CloudType.CIRRUS -> stringResource(Res.string.cloud_cirrus_desc)
                CloudType.NIMBUS -> stringResource(Res.string.cloud_nimbus_desc)
            }
            val res = when (content.type) {
                CloudType.CUMULUS -> Res.drawable.CUMULUS
                CloudType.STRATUS -> Res.drawable.STRATUS
                CloudType.CIRRUS -> Res.drawable.CIRRUS
                CloudType.NIMBUS -> Res.drawable.NIMBUS
            }
            Triple(content.type.name, desc, res)
        }
        is InfoContent.Star -> {
            val desc = when (content.type) {
                StarType.CAPELLA -> stringResource(Res.string.star_capella_desc)
                StarType.CASTOR -> stringResource(Res.string.star_castor_desc)
                StarType.SIRIUS -> stringResource(Res.string.star_sirius_desc)
                StarType.RIGEL -> stringResource(Res.string.star_rigel_desc)
            }
            val res = when (content.type) {
                StarType.CAPELLA -> Res.drawable.CAPELLA
                StarType.CASTOR -> Res.drawable.CASTOR
                StarType.SIRIUS -> Res.drawable.SIRIUS
                StarType.RIGEL -> Res.drawable.RIGEL
            }
            Triple(content.type.name, desc, res)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20) {
                        onClose()
                    }
                }
            },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = themeColors.skyBottom,
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
                text = title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
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

                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource(resource),
                    contentDescription = title,
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
