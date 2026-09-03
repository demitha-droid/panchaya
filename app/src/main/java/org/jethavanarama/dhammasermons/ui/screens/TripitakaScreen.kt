package org.jethavanarama.dhammasermons.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.model.TripitakaNode
import org.jethavanarama.dhammasermons.data.model.TripitakaParagraph
import org.jethavanarama.dhammasermons.data.model.TripitakaVaggaContent
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*

private enum class TripitakaLangMode(val label: String) {
    BOTH("පාළි + සිංහල"),
    SINHALA("සිංහල"),
    PALI("පාළි")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripitakaScreen(
    pitakas: List<TripitakaNode>,
    selectedPitaka: TripitakaNode?,
    selectedBook: TripitakaNode?,
    selectedVagga: TripitakaNode?,
    content: TripitakaVaggaContent?,
    isLoading: Boolean,
    searchQuery: String,
    searchResults: List<TripitakaNode>,
    onSelectPitaka: (TripitakaNode?) -> Unit,
    onSelectBook: (TripitakaNode?) -> Unit,
    onSelectVagga: (TripitakaNode?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    hasBottomMiniPlayer: Boolean = false
) {
    var fontSizeDelta by remember { mutableFloatStateOf(0f) }
    var langMode by remember { mutableStateOf(TripitakaLangMode.BOTH) }

    // Hardware back navigation handler
    BackHandler {
        when {
            searchQuery.isNotBlank() -> onSearchQueryChange("")
            selectedVagga != null -> onSelectVagga(null)
            selectedBook != null -> onSelectBook(null)
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(bottom = if (hasBottomMiniPlayer) 76.dp else 0.dp)
    ) {
        when {
            selectedVagga != null -> {
                // Native Sutta / Vagga Reader
                SuttaReaderView(
                    vagga = selectedVagga,
                    content = content,
                    isLoading = isLoading,
                    langMode = langMode,
                    fontSizeDelta = fontSizeDelta,
                    onLangModeChange = { langMode = it },
                    onIncreaseFontSize = { if (fontSizeDelta < 8f) fontSizeDelta += 2f },
                    onDecreaseFontSize = { if (fontSizeDelta > -4f) fontSizeDelta -= 2f },
                    onBackClick = { onSelectVagga(null) }
                )
            }
            searchQuery.isNotBlank() -> {
                // Search Results View
                TripitakaSearchResultsView(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    onSelectVagga = { vagga ->
                        onSearchQueryChange("")
                        onSelectVagga(vagga)
                    },
                    onClearSearch = { onSearchQueryChange("") }
                )
            }
            selectedBook != null -> {
                // Vaggas / Suttas list inside selected Book
                BookVaggasListView(
                    book = selectedBook,
                    onSelectVagga = onSelectVagga,
                    onBackClick = { onSelectBook(null) }
                )
            }
            else -> {
                // Main Pitaka Explorer
                PitakaExplorerView(
                    pitakas = pitakas,
                    selectedPitaka = selectedPitaka,
                    searchQuery = searchQuery,
                    isLoading = isLoading,
                    onSelectPitaka = onSelectPitaka,
                    onSelectBook = onSelectBook,
                    onSearchQueryChange = onSearchQueryChange,
                    onBack = onBack
                )
            }
        }
    }
}

/**
 * Explorer view: Pitaka selector tabs and Books list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PitakaExplorerView(
    pitakas: List<TripitakaNode>,
    selectedPitaka: TripitakaNode?,
    searchQuery: String,
    isLoading: Boolean,
    onSelectPitaka: (TripitakaNode?) -> Unit,
    onSelectBook: (TripitakaNode?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val str = strings

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_dhamma_chakra),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SaffronPrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = str.tripitakaTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SaffronPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        // Search Bar
        Surface(
            color = SurfaceCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SaffronPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = str.searchTripitakaPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pitaka Tabs Row (විනය, සුත්ත, අභිධම්ම)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pitakas) { pitaka ->
                val isSelected = pitaka.nodeId == selectedPitaka?.nodeId
                Surface(
                    onClick = { onSelectPitaka(pitaka) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) SaffronPrimary else SurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) SaffronGold else BorderDark),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pitaka.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color.Black.copy(alpha = 0.2f) else SurfaceDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pitaka.leafCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else SaffronGold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SaffronPrimary)
            }
        } else {
            // Books / Nikayas List
            val books = selectedPitaka?.children ?: emptyList()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(books) { book ->
                    BookCard(
                        book = book,
                        onClick = { onSelectBook(book) }
                    )
                }
            }
        }
    }
}

/**
 * Card representing a Book/Nikaya
 */
@Composable
private fun BookCard(
    book: TripitakaNode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SaffronGold.copy(alpha = 0.25f),
                                SaffronPrimary.copy(alpha = 0.10f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = SaffronPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${book.leafCount} සූත්‍ර / වග්ග",
                    style = MaterialTheme.typography.bodySmall,
                    color = SaffronGold
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * List of Vaggas/Suttas inside a selected Book
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookVaggasListView(
    book: TripitakaNode,
    onSelectVagga: (TripitakaNode) -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.leafCount} සූත්‍ර / වග්ග",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronGold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SaffronPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        val vaggas = remember(book) { book.allLeaves() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vaggas) { vagga ->
                VaggaListItem(
                    vagga = vagga,
                    onClick = { onSelectVagga(vagga) }
                )
            }
        }
    }
}

/**
 * Single Sutta/Vagga row item
 */
@Composable
private fun VaggaListItem(
    vagga: TripitakaNode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_dhamma_chakra),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SaffronGold)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vagga.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (vagga.hasSinhala && vagga.hasPali) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SaffronPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "පාළි + සිංහල",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Search results list view
 */
@Composable
private fun TripitakaSearchResultsView(
    searchQuery: String,
    searchResults: List<TripitakaNode>,
    onSelectVagga: (TripitakaNode) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ප්‍රතිඵල: ${searchResults.size}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaffronGold
                )
            )
            TextButton(onClick = onClearSearch) {
                Text(text = "සෙවුම ඉවත් කරන්න", color = TextSecondary)
            }
        }

        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "'$searchQuery' සඳහා කිසිදු සූත්‍රයක් හමු නොවීය",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(searchResults) { vagga ->
                    VaggaListItem(
                        vagga = vagga,
                        onClick = { onSelectVagga(vagga) }
                    )
                }
            }
        }
    }
}

/**
 * Native Sutta / Vagga Reader
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuttaReaderView(
    vagga: TripitakaNode,
    content: TripitakaVaggaContent?,
    isLoading: Boolean,
    langMode: TripitakaLangMode,
    fontSizeDelta: Float,
    onLangModeChange: (TripitakaLangMode) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = content?.title ?: vagga.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (content != null && (content.nikaya.isNotBlank() || content.book.isNotBlank())) {
                        Text(
                            text = listOf(content.nikaya, content.book).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = SaffronGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SaffronPrimary
                    )
                }
            },
            actions = {
                // Font Size Controls
                IconButton(onClick = onDecreaseFontSize) {
                    Text("A-", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onIncreaseFontSize) {
                    Text("A+", fontWeight = FontWeight.Bold, color = SaffronPrimary, fontSize = 15.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        // Language Filter Chips Bar (පාළි + සිංහල / සිංහල / පාළි)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TripitakaLangMode.entries.forEach { mode ->
                val isSelected = mode == langMode
                Surface(
                    onClick = { onLangModeChange(mode) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SaffronPrimary.copy(alpha = 0.2f) else SurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else BorderDark),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SaffronPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SaffronPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "සූත්‍රය පූරණය වෙමින් පවතී...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else if (content == null || content.paragraphs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "සූත්‍ර අන්තර්ගතය ලබා ගැනීමට නොහැකි විය.\nඅන්තර්ජාල සම්බන්ධතාවය පරීක්ෂා කරන්න.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Paragraphs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(content.paragraphs) { item ->
                    if (item.isHeading) {
                        // Section / Chapter Heading
                        SectionHeadingItem(
                            heading = item.headingText,
                            level = item.headingLevel
                        )
                    } else {
                        // Paragraph Card with dual view
                        ParagraphCard(
                            paragraph = item,
                            langMode = langMode,
                            fontSizeDelta = fontSizeDelta,
                            onCopy = { textToCopy ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Sutta Paragraph", textToCopy))
                                Toast.makeText(context, "පෙළ පිටපත් කරගන්නා ලදී", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section or Chapter Heading
 */
@Composable
private fun SectionHeadingItem(
    heading: String,
    level: Int
) {
    if (level == 1) {
        // Main Sutta Title / Chapter
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_dhamma_chakra),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SaffronPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SaffronGold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Subhead / Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(SaffronPrimary, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaffronPrimary
                )
            )
        }
    }
}

/**
 * Paragraph Card with Pali and/or Sinhala
 */
@Composable
private fun ParagraphCard(
    paragraph: TripitakaParagraph,
    langMode: TripitakaLangMode,
    fontSizeDelta: Float,
    onCopy: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Paragraph badge and Copy action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (paragraph.paraNumber.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SaffronPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "[ ${paragraph.paraNumber} ]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronGold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(
                    onClick = {
                        val text = when (langMode) {
                            TripitakaLangMode.PALI -> paragraph.paliText
                            TripitakaLangMode.SINHALA -> paragraph.sinhalaText
                            TripitakaLangMode.BOTH -> listOf(paragraph.paliText, paragraph.sinhalaText).filter { it.isNotBlank() }.joinToString("\n\n")
                        }
                        onCopy(text)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pali Text
            if ((langMode == TripitakaLangMode.BOTH || langMode == TripitakaLangMode.PALI) && paragraph.paliText.isNotBlank()) {
                Text(
                    text = paragraph.paliText,
                    fontSize = (15.5f + fontSizeDelta).sp,
                    lineHeight = (23f + fontSizeDelta * 1.3f).sp,
                    color = if (langMode == TripitakaLangMode.BOTH) SaffronGold.copy(alpha = 0.95f) else TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal
                )
            }

            // Divider if both are shown
            if (langMode == TripitakaLangMode.BOTH && paragraph.paliText.isNotBlank() && paragraph.sinhalaText.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderDark.copy(alpha = 0.6f)
                )
            }

            // Sinhala Text
            if ((langMode == TripitakaLangMode.BOTH || langMode == TripitakaLangMode.SINHALA) && paragraph.sinhalaText.isNotBlank()) {
                Text(
                    text = paragraph.sinhalaText,
                    fontSize = (15.5f + fontSizeDelta).sp,
                    lineHeight = (24f + fontSizeDelta * 1.3f).sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
