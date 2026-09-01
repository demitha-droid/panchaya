package org.jethavanarama.dhammasermons.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.data.model.DhammapadaGatha
import org.jethavanarama.dhammasermons.data.model.DhammapadaKatha
import org.jethavanarama.dhammasermons.data.model.DhammapadaVagga
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhammapadaScreen(
    vaggas: List<DhammapadaVagga>,
    selectedVagga: DhammapadaVagga?,
    searchQuery: String,
    searchResults: List<DhammapadaKatha>,
    isLoading: Boolean,
    playerState: PlayerUiState,
    onSelectVagga: (DhammapadaVagga?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayGatha: (Sermon, List<Sermon>) -> Unit
) {
    val str = strings

    // Handle system back button to go back from subcategory to main 26 Vaggas list
    BackHandler(enabled = selectedVagga != null) {
        onSelectVagga(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (selectedVagga == null) {
            // Mode A: All 26 Vaggas List / Grid
            AllVaggasView(
                vaggas = vaggas,
                searchQuery = searchQuery,
                searchResults = searchResults,
                isLoading = isLoading,
                onSelectVagga = onSelectVagga,
                onSearchQueryChange = onSearchQueryChange,
                onPlayGatha = onPlayGatha
            )
        } else {
            // Mode B: Selected Vagga Detail (Kathas, Gathas, Audio Player & Expandable Stories)
            VaggaDetailView(
                vagga = selectedVagga,
                allVaggas = vaggas,
                playerState = playerState,
                onBackClick = { onSelectVagga(null) },
                onSelectOtherVagga = { onSelectVagga(it) },
                onPlayGatha = onPlayGatha
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllVaggasView(
    vaggas: List<DhammapadaVagga>,
    searchQuery: String,
    searchResults: List<DhammapadaKatha>,
    isLoading: Boolean,
    onSelectVagga: (DhammapadaVagga) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayGatha: (Sermon, List<Sermon>) -> Unit
) {
    val str = strings
    val listState = rememberLazyListState()

    val filteredVaggas = remember(vaggas, searchQuery) {
        if (searchQuery.isBlank()) vaggas
        else vaggas.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.englishName.contains(searchQuery, ignoreCase = true) ||
            it.id.toString() == searchQuery.trim()
        }
    }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = str.dhammapadaTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = str.dhammapadaSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(SaffronGold.copy(alpha = 0.25f), SaffronPrimary.copy(alpha = 0.12f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = str.searchDhammapada,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SaffronPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Search Results Mode
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "සෙවුම් ප්‍රතිඵල (${searchResults.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SaffronGold
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ගාථා හෝ කතා හමු නොවීය",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { katha ->
                    KathaPostCard(
                        katha = katha,
                        vaggaName = "ධම්මපදය",
                        isCurrentPlaying = false,
                        isPlaying = false,
                        onPlayClick = { gatha, s -> onPlayGatha(s, listOf(s)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            // Section Title: 26 Vaggas
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = str.allVaggas,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Surface(
                        color = SaffronPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, SaffronPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "වර්ග 26",
                            color = SaffronPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 26 Vagga Cards with post illustrations
            items(filteredVaggas, key = { it.id }) { vagga ->
                VaggaCard(
                    vagga = vagga,
                    onClick = { onSelectVagga(vagga) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VaggaCard(
    vagga: DhammapadaVagga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstGatha = remember(vagga) {
        vagga.kathas.firstOrNull()?.gathas?.firstOrNull()
    }
    val imageUrl = remember(firstGatha) {
        firstGatha?.thumbnailUrl?.ifBlank { firstGatha.paintingUrl }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(0.8.dp, BorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vagga Post Illustration Thumbnail with badges
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = vagga.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Dark gradient overlay for badge readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Vagga Index Number Badge on Top-Left
                Surface(
                    color = SaffronPrimary,
                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = String.format("%02d", vagga.id),
                        color = BackgroundDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Gatha Range Badge on Bottom-Right
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "ගාථා ${vagga.gathaRange}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Vagga Title & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vagga.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (vagga.englishName.isNotBlank()) {
                    Text(
                        text = vagga.englishName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SaffronPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.6.dp, SaffronPrimary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "කථා ${vagga.kathaCount} ක්",
                            color = SaffronPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceHighlight)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = SaffronPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaggaDetailView(
    vagga: DhammapadaVagga,
    allVaggas: List<DhammapadaVagga>,
    playerState: PlayerUiState,
    onBackClick: () -> Unit,
    onSelectOtherVagga: (DhammapadaVagga) -> Unit,
    onPlayGatha: (Sermon, List<Sermon>) -> Unit
) {
    val listState = rememberLazyListState()

    // Create a playlist of all gathas in this vagga
    val vaggaSermons = remember(vagga) {
        vagga.kathas.flatMap { katha ->
            katha.gathas.map { gatha ->
                Sermon(
                    id = "dhammapada_${gatha.gathaNum}",
                    title = "${katha.kathaNum} ${katha.title}",
                    thero = "ධම්මපදය - ${vagga.name}",
                    source = SermonSource.DHAMMAPADA,
                    audioUrl = gatha.audioUrl,
                    thumbnailUrl = if (gatha.thumbnailUrl.isNotBlank()) gatha.thumbnailUrl else gatha.paintingUrl,
                    category = vagga.name
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Surface(
            color = SurfaceDark,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SaffronPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vagga.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ගාථා ${vagga.gathaRange} • කථා ${vagga.kathaCount} ක්",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Horizontal Vagga Selector Chips (1 to 26)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allVaggas, key = { it.id }) { v ->
                        val isSelected = v.id == vagga.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectOtherVagga(v) },
                            label = {
                                Text(
                                    text = "${v.id}. ${v.name.replace("^\\d+\\.\\s*".toRegex(), "")}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronPrimary,
                                selectedLabelColor = BackgroundDark,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) SaffronPrimary else BorderDark,
                                selectedBorderColor = SaffronPrimary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // List of Katha Post Cards
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            items(vagga.kathas, key = { it.id }) { katha ->
                val isCurrentPlaying = katha.gathas.any { g -> playerState.currentSermon?.id == "dhammapada_${g.gathaNum}" }
                val isPlaying = isCurrentPlaying && playerState.isPlaying

                KathaPostCard(
                    katha = katha,
                    vaggaName = vagga.name,
                    isCurrentPlaying = isCurrentPlaying,
                    isPlaying = isPlaying,
                    onPlayClick = { gatha, s ->
                        onPlayGatha(s, vaggaSermons)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun KathaPostCard(
    katha: DhammapadaKatha,
    vaggaName: String,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    onPlayClick: (DhammapadaGatha, Sermon) -> Unit,
    modifier: Modifier = Modifier
) {
    var isStoryExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isStoryExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) SurfaceHighlight else SurfaceCard
        ),
        border = BorderStroke(
            if (isCurrentPlaying) 1.2.dp else 0.8.dp,
            if (isCurrentPlaying) SaffronPrimary else BorderDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentPlaying) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Katha Title & Gatha Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = katha.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentPlaying) SaffronGold else TextPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val gathaNumsText = katha.gathas.joinToString(", ") { "${it.gathaNum}" }
                Surface(
                    color = SaffronPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, SaffronPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "ගාථා $gathaNumsText",
                        color = SaffronPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Each Gatha inside this story (Illustration, Audio Player, Pali & Sinhala)
            katha.gathas.forEachIndexed { idx, gatha ->
                if (idx > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val gathaSermon = remember(gatha, katha, vaggaName) {
                    Sermon(
                        id = "dhammapada_${gatha.gathaNum}",
                        title = "${katha.kathaNum} ${katha.title}",
                        thero = "ධම්මපදය - $vaggaName",
                        source = SermonSource.DHAMMAPADA,
                        audioUrl = gatha.audioUrl,
                        thumbnailUrl = if (gatha.thumbnailUrl.isNotBlank()) gatha.thumbnailUrl else gatha.paintingUrl,
                        category = vaggaName
                    )
                }

                GathaItemSection(
                    gatha = gatha,
                    sermon = gathaSermon,
                    isCurrentPlaying = isCurrentPlaying,
                    isPlaying = isPlaying,
                    onPlayClick = { onPlayClick(gatha, gathaSermon) }
                )
            }

            // Expandable Story Section
            if (katha.storyParagraphs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderDark.copy(alpha = 0.6f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Story Button
                Surface(
                    color = if (isStoryExpanded) SaffronPrimary.copy(alpha = 0.12f) else SurfaceHighlight.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, if (isStoryExpanded) SaffronPrimary.copy(alpha = 0.3f) else BorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isStoryExpanded = !isStoryExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isStoryExpanded) "සම්පූර්ණ කතාව හකුලන්න" else "සම්පූර්ණ කථා වස්තුව කියවන්න (${katha.storyParagraphs.size} ඡේද)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isStoryExpanded) SaffronPrimary else TextPrimary
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = if (isStoryExpanded) SaffronPrimary else TextMuted,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }

                // Expanded Story Text
                AnimatedVisibility(
                    visible = isStoryExpanded,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                color = SurfaceDark.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "සද්ධර්ම සාගර අටුවා කථා වස්තුව",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SaffronGold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        katha.storyParagraphs.forEachIndexed { pIdx, paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp,
                                    color = TextSecondary
                                ),
                                textAlign = TextAlign.Justify
                            )
                            if (pIdx < katha.storyParagraphs.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GathaItemSection(
    gatha: DhammapadaGatha,
    sermon: Sermon,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Thumbnail Illustration with Play overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (gatha.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = gatha.thumbnailUrl,
                        contentDescription = "Gatha ${gatha.gathaNum}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Dark semi-translucent duration/gatha tag
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "ගාථා ${gatha.gathaNum}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Inline Audio Player Bar
            Surface(
                color = SurfaceHighlight,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, if (isCurrentPlaying) SaffronPrimary.copy(alpha = 0.5f) else BorderDark),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPlayClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(SaffronGold, SaffronPrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Gatha Audio",
                            tint = BackgroundDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCurrentPlaying && isPlaying) "වාදනය වේ..." else "ගාථා සජ්ඣායනය",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentPlaying) SaffronGold else TextPrimary
                            )
                        )
                        Text(
                            text = "ශ්‍රවණය කිරීමට මෙහි ඔබන්න",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = SaffronPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pali Verse Box
        Surface(
            color = SurfaceDark.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, SaffronPrimary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = SaffronGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "පාලි ගාථාව",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronGold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                gatha.paliText.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sinhala Explanation Box
        Surface(
            color = SurfaceCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.6.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "සිංහල තේරුම",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = gatha.sinhalaText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = TextSecondary
                    )
                )
            }
        }
    }
}
