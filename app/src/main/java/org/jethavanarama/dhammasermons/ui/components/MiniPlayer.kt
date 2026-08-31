package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    playerState: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onSeekForward: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sermon = playerState.currentSermon ?: return
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    val maxDragPx = with(density) { 450.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var isDismissed by remember { mutableStateOf(false) }

    // Reset dismiss state if a new sermon is chosen
    LaunchedEffect(sermon.id) {
        offsetX.snapTo(0f)
        isDismissed = false
    }

    val progress = if (playerState.durationMs > 0) {
        (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Calculate alpha based on drag distance
    val dragAlpha = (1f - (abs(offsetX.value) / maxDragPx)).coerceIn(0.2f, 1f)

    val borderBrush = Brush.horizontalGradient(
        listOf(
            SaffronDark.copy(alpha = 0.4f),
            SaffronPrimary.copy(alpha = 0.8f),
            SaffronGold.copy(alpha = 0.9f),
            SaffronPrimary.copy(alpha = 0.5f)
        )
    )

    AnimatedVisibility(
        visible = !isDismissed,
        exit = fadeOut(tween(180)) + slideOutHorizontally(targetOffsetX = { if (offsetX.value >= 0) it else -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .alpha(dragAlpha)
                .draggable(
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + delta)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val currentOffset = offsetX.value
                            val shouldDismiss = abs(currentOffset) > dismissThresholdPx || abs(velocity) > 1000f
                            if (shouldDismiss) {
                                val target = if (currentOffset >= 0) maxDragPx else -maxDragPx
                                offsetX.animateTo(target, tween(160))
                                isDismissed = true
                                onDismiss()
                            } else {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                    }
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onClick),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, borderBrush),
                tonalElevation = 8.dp,
                shadowElevation = 14.dp,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Liquid Amber progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(SurfaceHighlight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SaffronDark, SaffronPrimary, SaffronGold)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail with Live Waveform Badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!sermon.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = sermon.thumbnailUrl,
                                    contentDescription = sermon.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (playerState.isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LiveWaveformVisualizer(
                                        isPlaying = true,
                                        barColor = SaffronGold,
                                        barCount = 3,
                                        barWidth = 2.dp,
                                        maxHeight = 12.dp,
                                        minHeight = 4.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title and Thero
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = sermon.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SaffronGold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sermon.thero,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = onSeekForward,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Play / Pause glowing dial
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(SaffronGold, SaffronPrimary)))
                        ) {
                            if (playerState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BackgroundDark,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = BackgroundDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
