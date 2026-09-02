package org.jethavanarama.dhammasermons.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jethavanarama.dhammasermons.DhammaApp
import org.jethavanarama.dhammasermons.data.db.AppDatabase
import org.jethavanarama.dhammasermons.data.downloader.ActiveDownloadItem
import org.jethavanarama.dhammasermons.data.downloader.SermonDownloadManager
import org.jethavanarama.dhammasermons.data.extractor.JethavanaramaExtractor
import org.jethavanarama.dhammasermons.data.extractor.PahuraYouTubeExtractor
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Language
import org.jethavanarama.dhammasermons.data.model.PaginatedSermons
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonCategory
import org.jethavanarama.dhammasermons.data.model.Thero
import org.jethavanarama.dhammasermons.data.model.DhammapadaKatha
import org.jethavanarama.dhammasermons.data.model.DhammapadaVagga
import org.jethavanarama.dhammasermons.data.repository.DhammapadaRepository
import org.jethavanarama.dhammasermons.player.AudioPlayerManager
import org.jethavanarama.dhammasermons.player.PlayerUiState
import org.jethavanarama.dhammasermons.ui.i18n.AppLanguage
import org.jethavanarama.dhammasermons.ui.theme.ThemeMode

@UnstableApi
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DhammaApp
    val playerManager: AudioPlayerManager = app.playerManager
    val downloadManager: SermonDownloadManager = app.downloadManager
    private val db = AppDatabase.getDatabase(application)
    private val sermonDao = db.sermonDao()
    private val prefs = application.getSharedPreferences("panchaya_settings", android.content.Context.MODE_PRIVATE)

    // Theme Mode
    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString("theme_mode", ThemeMode.LIGHT_KYOTO.name)
            ThemeMode.valueOf(saved ?: ThemeMode.LIGHT_KYOTO.name)
        } catch (e: Exception) {
            ThemeMode.LIGHT_KYOTO
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // App Interface Language (Sinhala, English, German, Hindi, Tamil)
    private val _appLanguage = MutableStateFlow(
        try {
            val saved = prefs.getString("app_language", AppLanguage.SINHALA.name)
            AppLanguage.valueOf(saved ?: AppLanguage.SINHALA.name)
        } catch (e: Exception) {
            AppLanguage.SINHALA
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.name).apply()
    }

    private val jethavanaramaExtractor = JethavanaramaExtractor()
    private val pahuraExtractor = PahuraYouTubeExtractor()

    // Player state
    val playerState: StateFlow<PlayerUiState> = playerManager.uiState
    val downloadStates: StateFlow<Map<String, DownloadState>> = downloadManager.downloadStates
    val activeDownloads: StateFlow<List<ActiveDownloadItem>> = downloadManager.activeDownloads

    // Home feed
    private val _homeSermons = MutableStateFlow<List<Sermon>>(emptyList())
    val homeSermons: StateFlow<List<Sermon>> = _homeSermons.asStateFlow()

    // Jethavanarama State & Pagination
    private val _jethavanaramaPaginated = MutableStateFlow(PaginatedSermons(emptyList()))
    val jethavanaramaPaginated: StateFlow<PaginatedSermons> = _jethavanaramaPaginated.asStateFlow()

    // Pahura State & Pagination
    private val _pahuraPaginated = MutableStateFlow(PaginatedSermons(emptyList()))
    val pahuraPaginated: StateFlow<PaginatedSermons> = _pahuraPaginated.asStateFlow()

    // Pahura Shorts State & Pagination
    private val _shortsPaginated = MutableStateFlow(PaginatedSermons(emptyList()))
    val shortsPaginated: StateFlow<PaginatedSermons> = _shortsPaginated.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _isJethavanaramaLoading = MutableStateFlow(false)
    val isJethavanaramaLoading: StateFlow<Boolean> = _isJethavanaramaLoading.asStateFlow()

    private val _isPahuraLoading = MutableStateFlow(false)
    val isPahuraLoading: StateFlow<Boolean> = _isPahuraLoading.asStateFlow()

    private val _isShortsLoading = MutableStateFlow(false)
    val isShortsLoading: StateFlow<Boolean> = _isShortsLoading.asStateFlow()

    // Filters for Jethavanarama
    val categories: List<SermonCategory> = jethavanaramaExtractor.defaultCategories
    val theros: List<Thero> = jethavanaramaExtractor.theros

    private val _selectedCategory = MutableStateFlow<SermonCategory?>(categories.firstOrNull())
    val selectedCategory: StateFlow<SermonCategory?> = _selectedCategory.asStateFlow()

    private val _selectedThero = MutableStateFlow<Thero?>(null)
    val selectedThero: StateFlow<Thero?> = _selectedThero.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(Language.SINHALA)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    // Offline downloads flow from Room DB
    val downloadedSermons: StateFlow<List<Sermon>> = sermonDao.getDownloadedSermons()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    // Dhammapada State
    val dhammapadaRepo = DhammapadaRepository(application)
    private val _dhammapadaVaggas = MutableStateFlow<List<DhammapadaVagga>>(emptyList())
    val dhammapadaVaggas: StateFlow<List<DhammapadaVagga>> = _dhammapadaVaggas.asStateFlow()

    private val _selectedDhammapadaVagga = MutableStateFlow<DhammapadaVagga?>(null)
    val selectedDhammapadaVagga: StateFlow<DhammapadaVagga?> = _selectedDhammapadaVagga.asStateFlow()

    private val _dhammapadaSearchQuery = MutableStateFlow("")
    val dhammapadaSearchQuery: StateFlow<String> = _dhammapadaSearchQuery.asStateFlow()

    private val _dhammapadaSearchResults = MutableStateFlow<List<DhammapadaKatha>>(emptyList())
    val dhammapadaSearchResults: StateFlow<List<DhammapadaKatha>> = _dhammapadaSearchResults.asStateFlow()

    private val _isDhammapadaLoading = MutableStateFlow(false)
    val isDhammapadaLoading: StateFlow<Boolean> = _isDhammapadaLoading.asStateFlow()

    fun selectDhammapadaVagga(vagga: DhammapadaVagga?) {
        _selectedDhammapadaVagga.value = vagga
    }

    fun setDhammapadaSearchQuery(query: String) {
        _dhammapadaSearchQuery.value = query
        if (query.isBlank()) {
            _dhammapadaSearchResults.value = emptyList()
        } else {
            viewModelScope.launch {
                _dhammapadaSearchResults.value = dhammapadaRepo.search(query)
            }
        }
    }

    fun loadDhammapada() {
        viewModelScope.launch {
            _isDhammapadaLoading.value = true
            val list = dhammapadaRepo.getVaggas()
            _dhammapadaVaggas.value = list
            _isDhammapadaLoading.value = false
        }
    }

    init {
        playerManager.ensureServiceStarted()
        refreshAllFeeds(forceNetworkRefresh = false)
        loadDhammapada()
        updateStorageUsage()
    }

    fun refreshAllFeeds(forceNetworkRefresh: Boolean = false) {
        if (forceNetworkRefresh) {
            pahuraExtractor.clearCache()
        }
        loadHomeFeed()
        loadJethavanaramaSermons(page = _jethavanaramaPaginated.value.currentPage.coerceAtLeast(1))
        loadPahuraSermons(page = _pahuraPaginated.value.currentPage.coerceAtLeast(1), forceRefresh = forceNetworkRefresh)
        loadPahuraShorts(page = _shortsPaginated.value.currentPage.coerceAtLeast(1), forceRefresh = forceNetworkRefresh)
    }

    fun loadHomeFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _isHomeLoading.value = true
            try {
                val jethLatest = jethavanaramaExtractor.fetchLatestSermons()
                val pahuraLatest = pahuraExtractor.fetchChannelSermons(1, forceRefresh = true).sermons

                val combined = mutableListOf<Sermon>()
                val maxLen = maxOf(jethLatest.size, pahuraLatest.size)
                for (i in 0 until maxLen) {
                    if (i < jethLatest.size) combined.add(jethLatest[i])
                    if (i < pahuraLatest.size) combined.add(pahuraLatest[i])
                }
                _homeSermons.value = combined
                resolveMissingHomeDurations(combined)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun loadJethavanaramaSermons(page: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            _isJethavanaramaLoading.value = true
            try {
                val cat = _selectedCategory.value
                val thero = _selectedThero.value
                val lang = _selectedLanguage.value

                val isAllCat = cat == null || cat.id == "all" || cat.id == "/sermons/sinhala"

                val result = if (thero != null) {
                    jethavanaramaExtractor.searchSermons(
                        theroId = thero.id,
                        categoryId = if (isAllCat) "" else cat!!.id,
                        page = page,
                        language = lang
                    )
                } else {
                    if (isAllCat) {
                        jethavanaramaExtractor.fetchCategorySermons("all", page, lang)
                    } else {
                        jethavanaramaExtractor.fetchCategorySermons(cat!!.slug, page, lang)
                    }
                }
                _jethavanaramaPaginated.value = result
                resolveMissingJethavanaramaDurations(result)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isJethavanaramaLoading.value = false
            }
        }
    }

    private fun resolveMissingJethavanaramaDurations(paginated: PaginatedSermons) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedSermons = paginated.sermons.map { sermon ->
                if (sermon.durationSeconds == 0L && !sermon.youtubeVideoId.isNullOrBlank()) {
                    val sec = jethavanaramaExtractor.resolveDurationSeconds(sermon.youtubeVideoId)
                    if (sec > 0L) sermon.copy(durationSeconds = sec) else sermon
                } else sermon
            }
            if (_jethavanaramaPaginated.value.currentPage == paginated.currentPage) {
                _jethavanaramaPaginated.value = _jethavanaramaPaginated.value.copy(sermons = updatedSermons)
            }
        }
    }

    private fun resolveMissingHomeDurations(sermons: List<Sermon>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = sermons.map { sermon ->
                if (sermon.durationSeconds == 0L && !sermon.youtubeVideoId.isNullOrBlank()) {
                    val sec = jethavanaramaExtractor.resolveDurationSeconds(sermon.youtubeVideoId)
                    if (sec > 0L) sermon.copy(durationSeconds = sec) else sermon
                } else sermon
            }
            _homeSermons.value = updated
        }
    }

    fun nextJethavanaramaPage() {
        val current = _jethavanaramaPaginated.value.currentPage
        if (_jethavanaramaPaginated.value.hasNextPage) {
            loadJethavanaramaSermons(current + 1)
        }
    }

    fun previousJethavanaramaPage() {
        val current = _jethavanaramaPaginated.value.currentPage
        if (current > 1) {
            loadJethavanaramaSermons(current - 1)
        }
    }

    fun loadPahuraSermons(page: Int = 1, forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isPahuraLoading.value = true
            try {
                val result = pahuraExtractor.fetchChannelSermons(page, forceRefresh = forceRefresh)
                _pahuraPaginated.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPahuraLoading.value = false
            }
        }
    }

    fun refreshPahura() {
        viewModelScope.launch(Dispatchers.IO) {
            _isPahuraLoading.value = true
            try {
                pahuraExtractor.clearCache()
                val result = pahuraExtractor.fetchChannelSermons(1, forceRefresh = true)
                _pahuraPaginated.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPahuraLoading.value = false
            }
        }
    }

    fun refreshShorts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isShortsLoading.value = true
            try {
                pahuraExtractor.clearCache()
                val result = pahuraExtractor.fetchChannelShorts(1, forceRefresh = true)
                _shortsPaginated.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isShortsLoading.value = false
            }
        }
    }

    fun refreshJethavanarama() {
        loadJethavanaramaSermons(page = _jethavanaramaPaginated.value.currentPage.coerceAtLeast(1))
    }

    fun nextPahuraPage() {
        val current = _pahuraPaginated.value.currentPage
        if (_pahuraPaginated.value.hasNextPage) {
            loadPahuraSermons(current + 1)
        }
    }

    fun previousPahuraPage() {
        val current = _pahuraPaginated.value.currentPage
        if (current > 1) {
            loadPahuraSermons(current - 1)
        }
    }

    fun loadPahuraShorts(page: Int = 1, forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isShortsLoading.value = true
            try {
                val result = pahuraExtractor.fetchChannelShorts(page, forceRefresh = forceRefresh)
                _shortsPaginated.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isShortsLoading.value = false
            }
        }
    }

    fun nextPahuraShortsPage() {
        val current = _shortsPaginated.value.currentPage
        if (_shortsPaginated.value.hasNextPage) {
            loadPahuraShorts(current + 1)
        }
    }

    fun previousPahuraShortsPage() {
        val current = _shortsPaginated.value.currentPage
        if (current > 1) {
            loadPahuraShorts(current - 1)
        }
    }

    fun selectCategory(category: SermonCategory) {
        _selectedCategory.value = category
        loadJethavanaramaSermons(page = 1)
    }

    fun selectThero(thero: Thero?) {
        _selectedThero.value = thero
        loadJethavanaramaSermons(page = 1)
    }

    fun selectLanguage(language: Language) {
        _selectedLanguage.value = language
        loadJethavanaramaSermons(page = 1)
    }

    fun playSermon(sermon: Sermon, queue: List<Sermon> = emptyList()) {
        playerManager.playSermon(sermon, queue)
    }

    fun playSermonById(sermonId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allCurrent = _homeSermons.value + _jethavanaramaPaginated.value.sermons + _pahuraPaginated.value.sermons + _shortsPaginated.value.sermons
            val target = allCurrent.firstOrNull { it.id == sermonId }
            if (target != null) {
                withContext(Dispatchers.Main) {
                    playerManager.playSermon(target, allCurrent)
                }
            } else {
                val jethLatest = try { jethavanaramaExtractor.fetchLatestSermons() } catch (e: Exception) { emptyList() }
                val pahuraLatest = try { pahuraExtractor.fetchChannelSermons(1, forceRefresh = true).sermons } catch (e: Exception) { emptyList() }
                val combined = (jethLatest + pahuraLatest).distinctBy { it.id }
                val found = combined.firstOrNull { it.id == sermonId }
                if (found != null) {
                    withContext(Dispatchers.Main) {
                        playerManager.playSermon(found, combined)
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun dismissMiniPlayer() {
        playerManager.dismissMiniPlayer()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        playerManager.seekRelative(offsetMs)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun startDownload(sermon: Sermon) {
        downloadManager.startDownload(sermon)
        updateStorageUsage()
    }

    fun resumeDownload(sermon: Sermon) {
        downloadManager.resumeDownload(sermon)
        updateStorageUsage()
    }

    fun cancelDownload(sermonId: String) {
        downloadManager.cancelDownload(sermonId)
        updateStorageUsage()
    }

    fun deleteDownload(sermon: Sermon) {
        downloadManager.deleteDownload(sermon)
        updateStorageUsage()
    }

    fun updateStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageUsedBytes.value = downloadManager.getStorageUsedBytes()
        }
    }
}
