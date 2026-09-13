package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerUiState,
    downloadState: DownloadState,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerSet: (Int) -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: (() -> Unit)? = null
) {
    val sermon = playerState.currentSermon ?: return
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }

    val currentPos = if (isUserSeeking) seekPositionMs else playerState.currentPositionMs
    val duration = playerState.durationMs

    val artworkBorder = BorderStroke(1.2.dp, BorderDark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundDark,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = BorderDark
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Surface(
                    color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.12f) else SaffronPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.3f) else SaffronPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (sermon.source == SermonSource.PAHURA_YOUTUBE) "පහුර YOUTUBE" else "ජෙතවනාරාමය",
                        color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed else SaffronPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDownloadClick) {
                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            CircularProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.size(22.dp),
                                color = SaffronPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        is DownloadState.Downloaded -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = AccentOfflineGreen
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download MP3",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Artwork with Delicate Sandstone Border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                border = artworkBorder,
                color = SurfaceCard,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!sermon.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = sermon.thumbnailUrl,
                            contentDescription = sermon.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.home_header),
                            fallback = painterResource(id = R.drawable.home_header)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = SaffronPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Title & Thero
            Text(
                text = sermon.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = sermon.thero,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = SaffronPrimary,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Audio Waveform Visualizer
            LiveWaveformVisualizer(
                isPlaying = playerState.isPlaying,
                modifier = Modifier.height(28.dp),
                barColor = SaffronPrimary,
                barCount = 18,
                barWidth = 3.dp,
                maxHeight = 26.dp,
                minHeight = 6.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Seek Slider with Terracotta Colors
            Slider(
                value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                onValueChange = { frac ->
                    isUserSeeking = true
                    seekPositionMs = (frac * duration).toLong()
                },
                onValueChangeFinished = {
                    onSeekTo(seekPositionMs)
                    isUserSeeking = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = SaffronPrimary,
                    activeTrackColor = SaffronPrimary,
                    inactiveTrackColor = SurfaceDark
                )
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls with Terracotta Dial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Replay 10s
                IconButton(
                    onClick = { onSeekRelative(-10000L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back 10s",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Large Terracotta Dial
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(SaffronGold, SaffronPrimary, SaffronDark)))
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Forward 30s
                IconButton(
                    onClick = { onSeekRelative(30000L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "Forward 30s",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Utility Controls (Speed & Sleep Timer Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Chip
                AssistChip(
                    onClick = { showSpeedDialog = true },
                    label = { Text("${playerState.playbackSpeed}x") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SurfaceCard,
                        labelColor = TextPrimary,
                        leadingIconContentColor = SaffronPrimary
                    ),
                    border = BorderStroke(1.dp, BorderDark)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Sleep Timer Chip
                AssistChip(
                    onClick = { showSleepTimerDialog = true },
                    label = {
                        Text(
                            if (playerState.sleepTimerSecondsRemaining > 0) {
                                val mins = playerState.sleepTimerSecondsRemaining / 60
                                val secs = playerState.sleepTimerSecondsRemaining % 60
                                String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                            } else "Sleep Timer"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SurfaceCard,
                        labelColor = TextPrimary,
                        leadingIconContentColor = SaffronPrimary
                    ),
                    border = BorderStroke(1.dp, BorderDark)
                )

                if (onAddToPlaylistClick != null) {
                    Spacer(modifier = Modifier.width(12.dp))

                    AssistChip(
                        onClick = onAddToPlaylistClick,
                        label = { Text(strings.addToPlaylist) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceCard,
                            labelColor = TextPrimary,
                            leadingIconContentColor = SaffronPrimary
                        ),
                        border = BorderStroke(1.dp, BorderDark)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        val str = strings
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(str.playbackSpeed, color = TextPrimary) },
            containerColor = SurfaceCard,
            text = {
                Column {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSpeedChange(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.playbackSpeed == speed,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SaffronPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${speed}x",
                                color = if (playerState.playbackSpeed == speed) SaffronPrimary else TextPrimary,
                                fontWeight = if (playerState.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text(str.close, color = SaffronPrimary)
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val str = strings
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text(str.sleepTimer, color = TextPrimary) },
            containerColor = SurfaceCard,
            text = {
                Column {
                    val options = listOf(
                        str.off to 0,
                        "15 ${str.minutes}" to 15,
                        "30 ${str.minutes}" to 30,
                        "45 ${str.minutes}" to 45,
                        "60 ${str.minutes}" to 60,
                        str.endOfSermon to -1
                    )
                    options.forEach { (label, minutes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSleepTimerSet(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(str.cancel, color = SaffronPrimary)
                }
            }
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remainingMinutes = minutes % 60
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, remainingMinutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
