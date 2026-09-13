package org.jethavanarama.dhammasermons.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.db.PlaylistEntity
import org.jethavanarama.dhammasermons.data.db.getDisplayName
import org.jethavanarama.dhammasermons.data.db.getDisplayDescription
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.components.LiveWaveformVisualizer
import org.jethavanarama.dhammasermons.ui.components.PLAYLIST_ICONS
import org.jethavanarama.dhammasermons.ui.components.parseHexColor
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    sermons: List<Sermon>,
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onEditPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onPlaySermon: (Sermon, List<Sermon>) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onRemoveSermon: (Sermon) -> Unit,
    onMoveSermon: (fromIndex: Int, toIndex: Int) -> Unit,
    hasBottomMiniPlayer: Boolean = false
) {
    val str = strings
    val accentColor = parseHexColor(playlist.colorAccentHex)
    val emoji = PLAYLIST_ICONS.firstOrNull { it.first == playlist.iconName }?.second ?: "☸"

    // Total Duration calculation
    val totalSeconds = remember(sermons) {
        sermons.sumOf { it.durationSeconds }
    }
    val totalDurationFormatted = remember(totalSeconds, str) {
        if (totalSeconds > 0) {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) "$hours ${str.hoursLabel} $minutes ${str.minutesLabel}" else "$minutes ${str.minutesLabel}"
        } else {
            ""
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.getDisplayName(str),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditPlaylist) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = str.editPlaylist,
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = if (hasBottomMiniPlayer) 76.dp else 0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Header Item
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(accentColor.copy(alpha = 0.5f), BorderDark)))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.2f),
                                        SurfaceCard
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Big Icon with Glow
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(accentColor.copy(alpha = 0.25f))
                                    .border(2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = playlist.getDisplayName(str),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (playlist.getDisplayDescription(str).isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = playlist.getDisplayDescription(str),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Metadata Pill
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SurfaceDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Text(
                                        text = "${sermons.size} ${str.tracksLabel}",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (totalDurationFormatted.isNotBlank()) {
                                    Surface(
                                        color = SurfaceDark,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderDark)
                                    ) {
                                        Text(
                                            text = totalDurationFormatted,
                                            color = accentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Play All & Shuffle Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onPlayAll,
                                    enabled = sermons.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = str.playAll, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onShuffle,
                                    enabled = sermons.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BorderDark),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = str.shuffle, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (sermons.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = str.emptyPlaylist,
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = str.emptyPlaylistHint,
                                color = TextMuted.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${str.sermonListLabel} (${sermons.size})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                    }
                }

                // Sermon List Items
                itemsIndexed(sermons, key = { _, sermon -> sermon.id }) { index, sermon ->
                    val isCurrent = playerState.currentSermon?.id == sermon.id

                    Surface(
                        color = if (isCurrent) SurfaceHighlight else SurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) accentColor else BorderDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySermon(sermon, sermons) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reorder Controls (Move Up / Move Down)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveSermon(index, index - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }

                                if (index < sermons.size - 1) {
                                    IconButton(
                                        onClick = { onMoveSermon(index, index + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!sermon.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = sermon.thumbnailUrl,
                                        contentDescription = sermon.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.home_header_icon),
                                        fallback = painterResource(id = R.drawable.home_header_icon)
                                    )
                                }

                                if (isCurrent && playerState.isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LiveWaveformVisualizer(isPlaying = true, barColor = Color.White, barCount = 3, modifier = Modifier.height(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Thero
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sermon.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isCurrent) accentColor else TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = sermon.thero,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    if (sermon.durationFormatted != null) {
                                        Text(
                                            text = "• ${sermon.durationFormatted}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Remove from Playlist button
                            IconButton(
                                onClick = { onRemoveSermon(sermon) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
