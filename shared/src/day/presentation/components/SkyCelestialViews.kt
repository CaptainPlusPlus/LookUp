package day.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import day.domain.CloudType
import day.domain.StarType
import org.jetbrains.compose.resources.painterResource
import shared.generated.resources.*

@Composable
fun StarView(
    starType: StarType,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = starType.name)
    val randomDelay = remember { (0..2000).random() }

    val opacity by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(randomDelay)
        ),
        label = "opacity"
    )

    val resource = when (starType) {
        StarType.CAPELLA -> Res.drawable.CAPELLA
        StarType.CASTOR -> Res.drawable.CASTOR
        StarType.SIRIUS -> Res.drawable.SIRIUS
        StarType.RIGEL -> Res.drawable.RIGEL
    }

    Image(
        painter = painterResource(resource),
        contentDescription = starType.name,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer(alpha = opacity)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

@Composable
fun CloudView(
    cloudTypes: List<CloudType>,
    scale: Float = 1f
) {
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
            .padding(horizontal = 8.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
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
                .aspectRatio(1.5f)
        )
    }
}
