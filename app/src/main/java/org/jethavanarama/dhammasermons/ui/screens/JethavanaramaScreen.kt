package org.jethavanarama.dhammasermons.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.data.model.*
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.components.DharmaChakraRefreshIndicator
import org.jethavanarama.dhammasermons.ui.components.PaginationBar
import org.jethavanarama.dhammasermons.ui.components.SermonCard
import org.jethavanarama.dhammasermons.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JethavanaramaScreen(
    paginatedSermons: PaginatedSermons,
    categories: List<SermonCategory>,
    theros: List<Thero>,
    selectedCategory: SermonCategory?,
    selectedThero: Thero?,
    selectedLanguage: Language,
    isLoading: Boolean,
    playerState: PlayerUiState,
    downloadStates: Map<String, DownloadState>,
    onSelectCategory: (SermonCategory) -> Unit,
    onSelectThero: (Thero?) -> Unit,
    onSelectLanguage: (Language) -> Unit,
    onPlaySermon: (Sermon, List<Sermon>) -> Unit,
    onDownloadSermon: (Sermon) -> Unit,
    onRefresh: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    var isTheroDropdownOpen by remember { mutableStateOf(false) }
    var isLangDropdownOpen by remember { mutableStateOf(false) }
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
            // Top Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ජෙතවනාරාමය",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Jethavanarama Buddhist Monastery",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filters Row (Theros & Language Dropdowns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Thero Selector Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isTheroDropdownOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedThero?.name ?: "සියලු ස්වාමීන් වහන්සේලා",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = if (selectedThero != null) SaffronGold else TextSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Thero",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isTheroDropdownOpen,
                                onDismissRequest = { isTheroDropdownOpen = false },
                                modifier = Modifier.background(SurfaceCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("සියලු ස්වාමීන් වහන්සේලා (All)", color = TextPrimary) },
                                    onClick = {
                                        onSelectThero(null)
                                        isTheroDropdownOpen = false
                                    },
                                    leadingIcon = if (selectedThero == null) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = SaffronPrimary) }
                                    } else null
                                )
                                theros.forEach { thero ->
                                    val isSelected = selectedThero?.id == thero.id
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = thero.name,
                                                color = if (isSelected) SaffronGold else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectThero(thero)
                                            isTheroDropdownOpen = false
                                        },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = SaffronPrimary) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Language Selector Dropdown
                        Box {
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark)),
                                modifier = Modifier.clickable { isLangDropdownOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedLanguage.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Language",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isLangDropdownOpen,
                                onDismissRequest = { isLangDropdownOpen = false },
                                modifier = Modifier.background(SurfaceCard)
                            ) {
                                Language.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayName, color = TextPrimary) },
                                        onClick = {
                                            onSelectLanguage(lang)
                                            isLangDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categories Filter Row
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "දේශනා කාණ්ඩ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = (selectedCategory?.id ?: "all") == category.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCategory(category) },
                                label = {
                                    Text(
                                        text = category.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = SaffronPrimary,
                                    selectedLabelColor = BackgroundDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) SaffronPrimary else BorderDark,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Selected Category Title & Sermon Count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCategory?.name ?: "දේශනා",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronGold
                        )
                    )

                    if (selectedThero != null) {
                        Surface(
                            color = SurfaceHighlight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedThero.name,
                                style = MaterialTheme.typography.labelSmall.copy(color = SaffronPrimary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Empty state
            if (!isLoading && paginatedSermons.sermons.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "දේශනා හමු නොවීය",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
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
