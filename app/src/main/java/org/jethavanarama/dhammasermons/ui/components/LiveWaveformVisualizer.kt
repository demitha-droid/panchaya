package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jethavanarama.dhammasermons.ui.theme.SaffronPrimary

@Composable
fun LiveWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SaffronPrimary,
    barCount: Int = 4,
    barWidth: Dp = 2.5.dp,
    maxHeight: Dp = 14.dp,
    minHeight: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = (0 until barCount).map { index ->
            if (isPlaying) {
                val duration = 400 + (index * 130) % 350
                val delay = (index * 90) % 250
                val animatedHeight by infiniteTransition.animateValue(
                    initialValue = minHeight,
                    targetValue = maxHeight,
                    typeConverter = Dp.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, delayMillis = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "BarHeight$index"
                )
                animatedHeight
            } else {
                minHeight
            }
        }

        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(h)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(barColor)
            )
        }
    }
}
