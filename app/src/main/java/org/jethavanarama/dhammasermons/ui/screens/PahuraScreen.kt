package org.jethavanarama.dhammasermons.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.PaginatedSermons
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.components.DharmaChakraRefreshIndicator
import org.jethavanarama.dhammasermons.ui.components.PaginationBar
import org.jethavanarama.dhammasermons.ui.components.SermonCard
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PahuraScreen(
    paginatedSermons: PaginatedSermons,
    isLoading: Boolean,
    playerState: PlayerUiState,
    downloadStates: Map<String, DownloadState>,
    onPlaySermon: (Sermon, List<Sermon>) -> Unit,
    onDownloadSermon: (Sermon) -> Unit,
    onRefresh: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onAddToPlaylistClick: ((Sermon) -> Unit)? = null
) {
    val str = strings
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()

    // Scroll to top when page changes
    LaunchedEffect(paginatedSermons.currentPage) {
        listState.animateScrollToItem(0)
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        state = refreshState,
        indicator = {
            DharmaChakraRefreshIndicator(
                state = refreshState,
                isRefreshing = isLoading
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Channel Banner Header
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(20.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(AccentPahuraRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subscriptions,
                                        contentDescription = null,
                                        tint = AccentPahuraRed,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = str.pahuraTitle,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = str.pahuraSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "නවතම දේශනා",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            // Empty state
            if (!isLoading && paginatedSermons.sermons.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "පහුර දේශනා ලබා ගැනීමට නොහැකි විය. කරුණාකර නැවත උත්සාහ කරන්න.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                        ) {
                            Text("නැවත උත්සාහ කරන්න (Retry)", color = BackgroundDark)
                        }
                    }
                }
            }

            // Sermons List
            items(paginatedSermons.sermons, key = { it.id }) { sermon ->
                val isCurrent = playerState.currentSermon?.id == sermon.id
                val isPlaying = isCurrent && playerState.isPlaying
                val dlState = downloadStates[sermon.id] ?: sermon.downloadState

                SermonCard(
                    sermon = sermon,
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    downloadState = dlState,
                    onPlayClick = { onPlaySermon(sermon, paginatedSermons.sermons) },
                    onDownloadClick = { onDownloadSermon(sermon) },
                    onAddToPlaylistClick = onAddToPlaylistClick?.let { { it(sermon) } },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Bottom Pagination Bar
            if (!isLoading && paginatedSermons.sermons.isNotEmpty()) {
                item {
                    PaginationBar(
                        currentPage = paginatedSermons.currentPage,
                        totalPages = paginatedSermons.totalPages,
                        hasNextPage = paginatedSermons.hasNextPage,
                        hasPreviousPage = paginatedSermons.hasPreviousPage,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage
                    )
                }
            }
        }
    }
}
