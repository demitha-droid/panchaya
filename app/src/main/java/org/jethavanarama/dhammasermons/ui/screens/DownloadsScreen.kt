package org.jethavanarama.dhammasermons.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.data.downloader.ActiveDownloadItem
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.components.DharmaChakraRefreshIndicator
import org.jethavanarama.dhammasermons.ui.components.SermonCard
import org.jethavanarama.dhammasermons.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadedSermons: List<Sermon>,
    activeDownloads: List<ActiveDownloadItem>,
    storageUsedBytes: Long,
    playerState: PlayerUiState,
    downloadStates: Map<String, DownloadState>,
    onPlaySermon: (Sermon, List<Sermon>) -> Unit,
    onDeleteDownload: (Sermon) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (Sermon) -> Unit,
    onRefresh: () -> Unit = {}
) {
    val str = strings
    var searchQuery by remember { mutableStateOf("") }
    val refreshState = rememberPullToRefreshState()
    var sermonToDelete by remember { mutableStateOf<Sermon?>(null) }

    val filteredSermons = remember(downloadedSermons, searchQuery) {
        if (searchQuery.isBlank()) downloadedSermons
        else downloadedSermons.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.thero.contains(searchQuery, ignoreCase = true)
        }
    }

    val storageFormatted = remember(storageUsedBytes) {
        val mb = storageUsedBytes / (1024.0 * 1024.0)
        if (mb >= 1024) {
            String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        state = refreshState,
        indicator = {
            DharmaChakraRefreshIndicator(
                state = refreshState,
                isRefreshing = false
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = str.downloadsTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = str.downloadsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Storage summary card
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SaffronPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = str.storageUsed,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = storageFormatted,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }

                            Surface(
                                color = SurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${downloadedSermons.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = SaffronGold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (downloadedSermons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(str.searchPlaceholder, color = TextMuted, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextMuted
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = BorderDark,
                                cursorColor = SaffronPrimary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            // Section 1: Active Downloads
            if (activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "බාගත වෙමින් පවතින (${activeDownloads.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SaffronGold
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                items(activeDownloads, key = { it.sermon.id }) { item ->
                    ActiveDownloadCard(
                        item = item,
                        onCancelClick = { onCancelDownload(item.sermon.id) },
                        onRetryClick = { onRetryDownload(item.sermon) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Section 2: Downloaded Sermons List
            item {
                Text(
                    text = if (searchQuery.isBlank()) "බාගත කළ දේශනා (${downloadedSermons.size})" else "සෙවුම් ප්‍රතිඵල (${filteredSermons.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Empty State
            if (downloadedSermons.isEmpty() && activeDownloads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "තවමත් බාගත කළ දේශනා නොමැත",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "ඕනෑම දේශනාවක Download බොත්තම ඔබා Offline ඇසීමට බාගත කරගන්න.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Downloaded Sermons Items
            items(filteredSermons, key = { it.id }) { sermon ->
                val isCurrent = playerState.currentSermon?.id == sermon.id
                val isPlaying = isCurrent && playerState.isPlaying
                val dlState = downloadStates[sermon.id] ?: sermon.downloadState

                DownloadedSermonRow(
                    sermon = sermon,
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    onPlayClick = { onPlaySermon(sermon, filteredSermons) },
                    onDeleteClick = { sermonToDelete = sermon },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (sermonToDelete != null) {
        val s = sermonToDelete!!
        AlertDialog(
            onDismissRequest = { sermonToDelete = null },
            title = {
                Text(
                    text = "දේශනාව ඉවත් කරන්නද?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "\"${s.title}\" ඔබගේ දුරකථනයෙන් ඉවත් කර ඉඩ නිදහස් කිරීමට අවශ්‍යද?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDownload(s)
                        sermonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPahuraRed)
                ) {
                    Text(str.delete, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sermonToDelete = null }) {
                    Text(str.cancel, color = TextMuted)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ActiveDownloadCard(
    item: ActiveDownloadItem,
    onCancelClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.sermon.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (item.isFailed) {
                    IconButton(
                        onClick = onRetryClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceHighlight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighlight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (item.isFailed) {
                Text(
                    text = "අසාර්ථක විය: ${item.errorMessage.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPahuraRed
                )
            } else {
                Column {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SaffronPrimary,
                        trackColor = BorderDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "බාගත වෙමින් පවතී...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "${(item.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedSermonRow(
    sermon: Sermon,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                } else {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = SaffronPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isCurrent) SaffronGold else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceHighlight.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Download",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
