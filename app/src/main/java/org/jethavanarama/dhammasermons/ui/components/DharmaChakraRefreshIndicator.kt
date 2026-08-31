package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jethavanarama.dhammasermons.ui.theme.BorderDark
import org.jethavanarama.dhammasermons.ui.theme.SaffronGold
import org.jethavanarama.dhammasermons.ui.theme.SaffronPrimary
import org.jethavanarama.dhammasermons.ui.theme.SurfaceCard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DharmaChakra(
    modifier: Modifier = Modifier,
    spokeCount: Int = 8,
    primaryColor: Color = SaffronGold,
    secondaryColor: Color = SaffronPrimary
) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = sizePx * 0.44f
        val innerRadius = outerRadius * 0.26f
        val strokeWidth = sizePx * 0.055f

        // 1. Outer Double Rim
        drawCircle(
            color = primaryColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = secondaryColor,
            radius = outerRadius * 0.82f,
            center = center,
            style = Stroke(width = strokeWidth * 0.5f)
        )

        // 2. Center Hub
        drawCircle(
            color = secondaryColor.copy(alpha = 0.35f),
            radius = innerRadius,
            center = center
        )
        drawCircle(
            color = primaryColor,
            radius = innerRadius,
            center = center,
            style = Stroke(width = strokeWidth * 0.8f)
        )
        drawCircle(
            color = primaryColor,
            radius = innerRadius * 0.45f,
            center = center
        )

        // 3. Spokes (Eightfold Noble Path)
        val spokeAngleStep = (2 * PI / spokeCount).toFloat()
        for (i in 0 until spokeCount) {
            val angle = i * spokeAngleStep
            val cosA = cos(angle.toDouble()).toFloat()
            val sinA = sin(angle.toDouble()).toFloat()

            val start = Offset(
                x = center.x + innerRadius * cosA,
                y = center.y + innerRadius * sinA
            )
            val end = Offset(
                x = center.x + (outerRadius * 0.82f) * cosA,
                y = center.y + (outerRadius * 0.82f) * sinA
            )

            drawLine(
                color = primaryColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth * 0.75f,
                cap = StrokeCap.Round
            )

            // Outer decorative rim jewels
            val jewelCenter = Offset(
                x = center.x + (outerRadius + strokeWidth * 0.2f) * cosA,
                y = center.y + (outerRadius + strokeWidth * 0.2f) * sinA
            )
            drawCircle(
                color = secondaryColor,
                radius = strokeWidth * 0.55f,
                center = jewelCenter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.DharmaChakraRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    // Continuous smooth infinite rotation transition
    val infiniteTransition = rememberInfiniteTransition(label = "DharmaChakraSpin")
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DharmaChakraRotation"
    )

    val pullFraction = state.distanceFraction.coerceIn(0f, 1.5f)
    val scale = if (isRefreshing) 1f else (pullFraction.coerceIn(0f, 1f))
    val alpha = if (isRefreshing) 1f else (pullFraction.coerceIn(0.2f, 1f))
    
    // Always rotate actively: When pulling, combines pull distance + continuous spin so it never freezes
    val rotation = if (isRefreshing) {
        continuousRotation
    } else {
        (pullFraction * 360f) + (continuousRotation * (pullFraction * 0.5f))
    }

    if (pullFraction > 0.05f || isRefreshing) {
        Box(
            modifier = modifier
                .align(Alignment.TopCenter)
                .padding(top = (pullFraction * 24).dp + 12.dp)
                .scale(scale)
                .alpha(alpha)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = SaffronPrimary,
                    spotColor = SaffronGold
                )
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.5.dp, Brush.radialGradient(listOf(SaffronGold, BorderDark)), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            DharmaChakra(
                modifier = Modifier
                    .size(size)
                    .rotate(rotation),
                spokeCount = 8,
                primaryColor = SaffronGold,
                secondaryColor = SaffronPrimary
            )
        }
    }
}
