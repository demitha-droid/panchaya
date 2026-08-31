package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.jethavanarama.dhammasermons.ui.theme.*

@Composable
fun SermonCard(
    sermon: Sermon,
    isPlaying: Boolean,
    isCurrent: Boolean,
    downloadState: DownloadState,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBorder = if (isCurrent) {
        BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    SaffronPrimary,
                    SaffronGold,
                    SaffronDark
                )
            )
        )
    } else {
        BorderStroke(width = 1.dp, color = BorderDark)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlayClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SurfaceHighlight else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp),
        shape = RoundedCornerShape(18.dp),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay & Live Waveform
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                // Background icon placeholder
                Icon(
                    imageVector = if (sermon.source == SermonSource.PAHURA_YOUTUBE) Icons.Default.Subscriptions else Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.35f) else SaffronPrimary.copy(alpha = 0.35f),
                    modifier = Modifier.size(34.dp)
                )

                if (!sermon.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = sermon.thumbnailUrl,
                        contentDescription = sermon.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Play / Pause overlay if active
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(SaffronGold, SaffronPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Playback",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Duration badge on thumbnail (bottom-end)
                if (sermon.durationFormatted != null && !isCurrent) {
                    Surface(
                        color = TextPrimary.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = sermon.durationFormatted!!,
                            color = BackgroundDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Source badge, Live waveform & Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.12f) else SaffronPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.3f) else SaffronPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (sermon.source == SermonSource.PAHURA_YOUTUBE) "පහුර" else "ජෙතවනාරාමය",
                            color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed else SaffronPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isCurrent && isPlaying) {
                        LiveWaveformVisualizer(
                            isPlaying = true,
                            barColor = SaffronPrimary,
                            barCount = 4,
                            barWidth = 2.dp,
                            maxHeight = 12.dp,
                            minHeight = 4.dp
                        )
                    } else if (sermon.durationFormatted != null) {
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, BorderDark)
                        ) {
                            Text(
                                text = sermon.durationFormatted!!,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!sermon.date.isNullOrBlank()) {
                        Text(
                            text = sermon.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Title
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isCurrent) SaffronPrimary else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Thero Name
                Text(
                    text = sermon.thero,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download Action Button
            Box(contentAlignment = Alignment.Center) {
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        if (downloadState.progress > 0f) {
                            CircularProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.size(32.dp),
                                color = SaffronPrimary,
                                strokeWidth = 2.5.dp,
                                trackColor = BorderDark
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = SaffronPrimary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                    is DownloadState.Downloaded -> {
                        IconButton(
                            onClick = { /* Already downloaded */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = AccentOfflineGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    else -> {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download MP3",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
