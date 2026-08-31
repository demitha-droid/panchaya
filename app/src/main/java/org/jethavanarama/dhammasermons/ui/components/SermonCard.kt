package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlayClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SurfaceHighlight else SurfaceCard
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaffronPrimary.copy(alpha = 0.6f))) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay & Duration badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                // Saffron / Red Source icon placeholder
                Icon(
                    imageVector = if (sermon.source == SermonSource.PAHURA_YOUTUBE) Icons.Default.Subscriptions else Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.4f) else SaffronPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
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
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = SaffronGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Duration badge on thumbnail (bottom-end)
                if (sermon.durationFormatted != null && !isCurrent) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = sermon.durationFormatted!!,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Source badge, Date & Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed.copy(alpha = 0.15f) else AccentJethavanaramaGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (sermon.source == SermonSource.PAHURA_YOUTUBE) "පහුර" else "ජෙතවනාරාමය",
                            color = if (sermon.source == SermonSource.PAHURA_YOUTUBE) AccentPahuraRed else AccentJethavanaramaGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (sermon.durationFormatted != null) {
                        Surface(
                            color = SurfaceHighlight,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = sermon.durationFormatted!!,
                                color = SaffronGold,
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

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isCurrent) SaffronGold else TextPrimary,
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
                                .background(SurfaceHighlight.copy(alpha = 0.5f))
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
