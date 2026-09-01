package org.jethavanarama.dhammasermons.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.PaginatedSermons
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.components.DharmaChakraRefreshIndicator
import org.jethavanarama.dhammasermons.ui.components.PaginationBar
import org.jethavanarama.dhammasermons.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    paginatedShorts: PaginatedSermons,
    isLoading: Boolean,
    playerState: PlayerUiState,
    downloadStates: Map<String, DownloadState>,
    onPlayAudioOnly: (Sermon, List<Sermon>) -> Unit,
    onDownloadSermon: (Sermon) -> Unit,
    onRefresh: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    var selectedVideoShort by remember { mutableStateOf<Sermon?>(null) }

    LaunchedEffect(paginatedShorts.currentPage) {
        gridState.animateScrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner
                item(span = { GridItemSpan(2) }) {
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
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AccentPahuraRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = AccentPahuraRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "කෙටි දේශනා (Shorts)",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Pahura Official Shorts",
                                        style = MaterialTheme.typography.bodyMedium,
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
                }

                // Empty state
                if (!isLoading && paginatedShorts.sermons.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Shorts දේශනා හමු නොවීය. නැවත උත්සාහ කරන්න.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                            ) {
                                Text("නැවත උත්සාහ කරන්න", color = BackgroundDark)
                            }
                        }
                    }
                }

                // Shorts Grid Items
                items(paginatedShorts.sermons, key = { it.id }) { shortSermon ->
                    val isCurrent = playerState.currentSermon?.id == shortSermon.id
                    val isPlaying = isCurrent && playerState.isPlaying
                    val dlState = downloadStates[shortSermon.id] ?: shortSermon.downloadState

                    ShortGridCard(
                        sermon = shortSermon,
                        isPlaying = isPlaying,
                        isCurrent = isCurrent,
                        downloadState = dlState,
                        onWatchVideo = { selectedVideoShort = shortSermon },
                        onPlayAudio = { onPlayAudioOnly(shortSermon, paginatedShorts.sermons) },
                        onDownload = { onDownloadSermon(shortSermon) }
                    )
                }

                // Bottom Pagination Bar
                if (!isLoading && paginatedShorts.sermons.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        PaginationBar(
                            currentPage = paginatedShorts.currentPage,
                            totalPages = paginatedShorts.totalPages,
                            hasNextPage = paginatedShorts.hasNextPage,
                            hasPreviousPage = paginatedShorts.hasPreviousPage,
                            onPreviousPage = onPreviousPage,
                            onNextPage = onNextPage
                        )
                    }
                }
            }
        }

        // Fullscreen In-App HTML5 Video Player Modal
        if (selectedVideoShort != null) {
            val short = selectedVideoShort!!
            val curDlState = downloadStates[short.id] ?: short.downloadState

            ShortsVideoDialog(
                sermon = short,
                downloadState = curDlState,
                onDismiss = { selectedVideoShort = null },
                onPlayAudioOnly = {
                    onPlayAudioOnly(short, paginatedShorts.sermons)
                    selectedVideoShort = null
                },
                onDownload = { onDownloadSermon(short) }
            )
        }
    }
}

@Composable
private fun ShortGridCard(
    sermon: Sermon,
    isPlaying: Boolean,
    isCurrent: Boolean,
    downloadState: DownloadState,
    onWatchVideo: () -> Unit,
    onPlayAudio: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onWatchVideo),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaffronPrimary)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 9:16 Vertical Thumbnail
            if (!sermon.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = sermon.thumbnailUrl,
                    contentDescription = sermon.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dark Vertical Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top Badges (Shorts icon badge & Audio Playing indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AccentPahuraRed,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SHORT",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isCurrent) {
                    Surface(
                        color = SaffronPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.Pause,
                            contentDescription = "Playing",
                            tint = BackgroundDark,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Center Play Icon Indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Bottom Information & Quick Action Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row (Audio-Only & Download)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Only Button
                    Surface(
                        color = SurfaceDark.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onPlayAudio)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Audio Only",
                                tint = SaffronPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Audio",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Download Button
                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            CircularProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.size(24.dp),
                                color = SaffronPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        is DownloadState.Downloaded -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = AccentOfflineGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        else -> {
                            IconButton(
                                onClick = onDownload,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download MP3",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShortsVideoDialog(
    sermon: Sermon,
    downloadState: DownloadState,
    onDismiss: () -> Unit,
    onPlayAudioOnly: () -> Unit,
    onDownload: () -> Unit
) {
    val videoId = sermon.youtubeVideoId.orEmpty()
    val htmlData = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe 
                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Embedded Video Player
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Close Button & Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    color = AccentPahuraRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Short Video",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Actions Bar
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sermon.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Listen Audio
                    Button(
                        onClick = onPlayAudioOnly,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = BackgroundDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Download MP3
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download MP3",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
