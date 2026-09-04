package org.jethavanarama.dhammasermons

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.ui.components.FullPlayerSheet
import org.jethavanarama.dhammasermons.ui.components.MiniPlayer
import org.jethavanarama.dhammasermons.ui.components.SettingsSheet
import org.jethavanarama.dhammasermons.ui.i18n.AppLanguage
import org.jethavanarama.dhammasermons.ui.i18n.AppStrings
import org.jethavanarama.dhammasermons.ui.i18n.LocalAppStrings
import org.jethavanarama.dhammasermons.ui.i18n.getStrings
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.screens.DhammapadaScreen
import org.jethavanarama.dhammasermons.ui.screens.DownloadsScreen
import org.jethavanarama.dhammasermons.ui.screens.HomeScreen
import org.jethavanarama.dhammasermons.ui.screens.JethavanaramaScreen
import org.jethavanarama.dhammasermons.ui.screens.PahuraScreen
import org.jethavanarama.dhammasermons.ui.screens.ShortsScreen
import org.jethavanarama.dhammasermons.ui.screens.TripitakaScreen
import org.jethavanarama.dhammasermons.ui.theme.*
import org.jethavanarama.dhammasermons.ui.viewmodel.MainViewModel

enum class NavTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home),
    JETHAVANARAMA(Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement),
    PAHURA(Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
    SHORTS(Icons.Filled.PlayCircleFilled, Icons.Outlined.PlayCircleOutline),
    DHAMMAPADA(Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    DOWNLOADS(Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload);

    fun getTitle(str: AppStrings): String = when (this) {
        HOME -> str.tabHome
        JETHAVANARAMA -> str.tabJethavanarama
        PAHURA -> str.tabPahura
        SHORTS -> str.tabShorts
        DHAMMAPADA -> str.tabDhammapada
        DOWNLOADS -> str.tabDownloads
    }
}

@UnstableApi
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.playerManager.ensureServiceStarted()
        handleNotificationIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val currentStrings = appLanguage.getStrings()

            CompositionLocalProvider(LocalAppStrings provides currentStrings) {
                DhammaSermonsTheme(themeMode = themeMode) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val sermonId = intent?.getStringExtra(org.jethavanarama.dhammasermons.notifications.NewSermonNotificationManager.EXTRA_SERMON_ID)
        val autoPlay = intent?.getBooleanExtra(org.jethavanarama.dhammasermons.notifications.NewSermonNotificationManager.EXTRA_AUTO_PLAY, false) ?: false
        if (!sermonId.isNullOrBlank() && autoPlay) {
            viewModel.playSermonById(sermonId)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            viewModel.playerManager.saveCurrentPositionNow()
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            viewModel.playerManager.saveCurrentPositionNow()
        } catch (e: Exception) {
            // Non-fatal
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var isFullPlayerVisible by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isTripitakaVisible by remember { mutableStateOf(false) }

    val homeSermons by viewModel.homeSermons.collectAsState()
    val jethavanaramaPaginated by viewModel.jethavanaramaPaginated.collectAsState()
    val pahuraPaginated by viewModel.pahuraPaginated.collectAsState()
    val shortsPaginated by viewModel.shortsPaginated.collectAsState()
    val downloadedSermons by viewModel.downloadedSermons.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsState()

    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val isJethavanaramaLoading by viewModel.isJethavanaramaLoading.collectAsState()
    val isPahuraLoading by viewModel.isPahuraLoading.collectAsState()
    val isShortsLoading by viewModel.isShortsLoading.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedThero by viewModel.selectedThero.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    val dhammapadaVaggas by viewModel.dhammapadaVaggas.collectAsState()
    val selectedDhammapadaVagga by viewModel.selectedDhammapadaVagga.collectAsState()
    val dhammapadaSearchQuery by viewModel.dhammapadaSearchQuery.collectAsState()
    val dhammapadaSearchResults by viewModel.dhammapadaSearchResults.collectAsState()
    val isDhammapadaLoading by viewModel.isDhammapadaLoading.collectAsState()

    val playerState by viewModel.playerState.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    val tripitakaPitakas by viewModel.tripitakaPitakas.collectAsState()
    val selectedPitaka by viewModel.selectedPitaka.collectAsState()
    val selectedTripitakaBook by viewModel.selectedTripitakaBook.collectAsState()
    val selectedTripitakaVagga by viewModel.selectedTripitakaVagga.collectAsState()
    val tripitakaContent by viewModel.tripitakaContent.collectAsState()
    val isTripitakaLoading by viewModel.isTripitakaLoading.collectAsState()
    val tripitakaSearchQuery by viewModel.tripitakaSearchQuery.collectAsState()
    val tripitakaSearchResults by viewModel.tripitakaSearchResults.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 4.dp
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab && !isTripitakaVisible
                    val badgeCount = if (tab == NavTab.DOWNLOADS) activeDownloads.size else 0
                    val tabTitle = tab.getTitle(strings)

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            isTripitakaVisible = false
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(containerColor = SaffronPrimary, contentColor = BackgroundDark) {
                                            Text("$badgeCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tabTitle
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tabTitle,
                                fontSize = 8.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SaffronPrimary,
                            selectedTextColor = SaffronPrimary,
                            indicatorColor = SurfaceHighlight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Current Selected Screen
            when (selectedTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        sermons = homeSermons,
                        isLoading = isHomeLoading,
                        playerState = playerState,
                        downloadStates = downloadStates,
                        onPlaySermon = { sermon, queue -> viewModel.playSermon(sermon, queue) },
                        onDownloadSermon = { sermon -> viewModel.startDownload(sermon) },
                        onRefresh = { viewModel.loadHomeFeed() },
                        onOpenSettings = { showSettingsSheet = true },
                        onNavigateToJethavanarama = {
                            selectedTab = NavTab.JETHAVANARAMA
                            isTripitakaVisible = false
                        },
                        onNavigateToPahura = {
                            selectedTab = NavTab.PAHURA
                            isTripitakaVisible = false
                        },
                        onNavigateToDhammapada = {
                            selectedTab = NavTab.DHAMMAPADA
                            isTripitakaVisible = false
                        },
                        onOpenTripitaka = { isTripitakaVisible = true }
                    )
                }
                NavTab.DHAMMAPADA -> {
                    DhammapadaScreen(
                        vaggas = dhammapadaVaggas,
                        selectedVagga = selectedDhammapadaVagga,
                        searchQuery = dhammapadaSearchQuery,
                        searchResults = dhammapadaSearchResults,
                        isLoading = isDhammapadaLoading,
                        playerState = playerState,
                        onSelectVagga = { vagga -> viewModel.selectDhammapadaVagga(vagga) },
                        onSearchQueryChange = { q -> viewModel.setDhammapadaSearchQuery(q) },
                        onPlayGatha = { sermon, queue -> viewModel.playSermon(sermon, queue) }
                    )
                }
                NavTab.JETHAVANARAMA -> {
                    JethavanaramaScreen(
                        paginatedSermons = jethavanaramaPaginated,
                        categories = viewModel.categories,
                        theros = viewModel.theros,
                        selectedCategory = selectedCategory,
                        selectedThero = selectedThero,
                        selectedLanguage = selectedLanguage,
                        isLoading = isJethavanaramaLoading,
                        playerState = playerState,
                        downloadStates = downloadStates,
                        onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                        onSelectThero = { thero -> viewModel.selectThero(thero) },
                        onSelectLanguage = { lang -> viewModel.selectLanguage(lang) },
                        onPlaySermon = { sermon, queue -> viewModel.playSermon(sermon, queue) },
                        onDownloadSermon = { sermon -> viewModel.startDownload(sermon) },
                        onRefresh = { viewModel.refreshJethavanarama() },
                        onNextPage = { viewModel.nextJethavanaramaPage() },
                        onPreviousPage = { viewModel.previousJethavanaramaPage() },
                        onOpenSettings = { showSettingsSheet = true }
                    )
                }
                NavTab.PAHURA -> {
                    PahuraScreen(
                        paginatedSermons = pahuraPaginated,
                        isLoading = isPahuraLoading,
                        playerState = playerState,
                        downloadStates = downloadStates,
                        onPlaySermon = { sermon, queue -> viewModel.playSermon(sermon, queue) },
                        onDownloadSermon = { sermon -> viewModel.startDownload(sermon) },
                        onRefresh = { viewModel.refreshPahura() },
                        onNextPage = { viewModel.nextPahuraPage() },
                        onPreviousPage = { viewModel.previousPahuraPage() }
                    )
                }
                NavTab.SHORTS -> {
                    ShortsScreen(
                        paginatedShorts = shortsPaginated,
                        isLoading = isShortsLoading,
                        playerState = playerState,
                        downloadStates = downloadStates,
                        onPlayAudioOnly = { sermon, queue -> viewModel.playSermon(sermon, queue) },
                        onDownloadSermon = { sermon -> viewModel.startDownload(sermon) },
                        onRefresh = { viewModel.refreshShorts() },
                        onNextPage = { viewModel.nextPahuraShortsPage() },
                        onPreviousPage = { viewModel.previousPahuraShortsPage() }
                    )
                }
                NavTab.DOWNLOADS -> {
                    DownloadsScreen(
                        downloadedSermons = downloadedSermons,
                        activeDownloads = activeDownloads,
                        storageUsedBytes = storageUsedBytes,
                        playerState = playerState,
                        downloadStates = downloadStates,
                        onPlaySermon = { sermon, queue -> viewModel.playSermon(sermon, queue) },
                        onDeleteDownload = { sermon -> viewModel.deleteDownload(sermon) },
                        onCancelDownload = { id -> viewModel.cancelDownload(id) },
                        onRetryDownload = { sermon -> viewModel.resumeDownload(sermon) },
                        onRefresh = { viewModel.loadHomeFeed() }
                    )
                }
            }

            // Tripitaka In-App Screen
            if (isTripitakaVisible) {
                TripitakaScreen(
                    pitakas = tripitakaPitakas,
                    selectedPitaka = selectedPitaka,
                    selectedBook = selectedTripitakaBook,
                    selectedVagga = selectedTripitakaVagga,
                    content = tripitakaContent,
                    isLoading = isTripitakaLoading,
                    searchQuery = tripitakaSearchQuery,
                    searchResults = tripitakaSearchResults,
                    onSelectPitaka = { viewModel.selectPitaka(it) },
                    onSelectBook = { viewModel.selectTripitakaBook(it) },
                    onSelectVagga = { viewModel.selectTripitakaVagga(it) },
                    onSearchQueryChange = { viewModel.setTripitakaSearchQuery(it) },
                    onBack = { isTripitakaVisible = false },
                    hasBottomMiniPlayer = playerState.currentSermon != null
                )
            }

            // Floating Mini Player
            if (playerState.currentSermon != null) {
                MiniPlayer(
                    playerState = playerState,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekBack = { viewModel.seekRelative(-10000L) },
                    onSeekForward = { viewModel.seekRelative(10000L) },
                    onDismiss = { viewModel.dismissMiniPlayer() },
                    onClick = { isFullPlayerVisible = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Full Player Modal Bottom Sheet
            if (isFullPlayerVisible && playerState.currentSermon != null) {
                val curSermon = playerState.currentSermon!!
                val dlState = downloadStates[curSermon.id] ?: curSermon.downloadState
                FullPlayerSheet(
                    playerState = playerState,
                    downloadState = dlState,
                    onDismiss = { isFullPlayerVisible = false },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onSeekRelative = { offset -> viewModel.seekRelative(offset) },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onSleepTimerSet = { mins -> viewModel.setSleepTimer(mins) },
                    onDownloadClick = { viewModel.startDownload(curSermon) }
                )
            }

            // Settings Modal Bottom Sheet
            if (showSettingsSheet) {
                val currentThemeMode by viewModel.themeMode.collectAsState()
                val currentLanguage by viewModel.appLanguage.collectAsState()
                val saveDownloadsAsMp3 by viewModel.saveDownloadsAsMp3.collectAsState()
                SettingsSheet(
                    currentThemeMode = currentThemeMode,
                    onThemeModeSelect = { viewModel.setThemeMode(it) },
                    currentLanguage = currentLanguage,
                    onLanguageSelect = { viewModel.setAppLanguage(it) },
                    saveDownloadsAsMp3 = saveDownloadsAsMp3,
                    onSaveDownloadsAsMp3Toggle = { viewModel.setSaveDownloadsAsMp3(it) },
                    onRefreshFeeds = { viewModel.refreshAllFeeds(forceNetworkRefresh = true) },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}
