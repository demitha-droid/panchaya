package org.jethavanarama.dhammasermons.player

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jethavanarama.dhammasermons.data.extractor.AudioExtractionResult
import org.jethavanarama.dhammasermons.data.extractor.JethavanaramaExtractor
import org.jethavanarama.dhammasermons.data.extractor.PahuraYouTubeExtractor
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.jethavanarama.dhammasermons.notifications.SermonResumeReminderManager
import java.util.concurrent.TimeUnit

data class PlayerUiState(
    val currentSermon: Sermon? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutesLeft: Int? = null,
    val sleepTimerSecondsRemaining: Long = 0L,
    val queue: List<Sermon> = emptyList(),
    val errorMessage: String? = null
)

@UnstableApi
class AudioPlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    val progressManager = PlaybackProgressManager(context)

    private val jethavanaramaExtractor = JethavanaramaExtractor()
    private val pahuraExtractor = PahuraYouTubeExtractor()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .setUsage(C.USAGE_MEDIA)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        sessionPlayStartTime = System.currentTimeMillis()
                        // User is actively listening; dismiss any active reminder notification
                        SermonResumeReminderManager.dismissNotification(context)
                        startProgressTracker()
                    } else {
                        if (sessionPlayStartTime > 0L) {
                            sessionListeningTimeMs += (System.currentTimeMillis() - sessionPlayStartTime)
                            sessionPlayStartTime = 0L
                        }
                        saveCurrentPositionNow()
                        stopProgressTracker()

                        val curSermon = _uiState.value.currentSermon
                        if (curSermon != null) {
                            val curPos = player.currentPosition.coerceAtLeast(0L)
                            val dur = player.duration.coerceAtLeast(0L)
                            SermonResumeReminderManager.checkAndScheduleOnStop(
                                context = context,
                                sermon = curSermon,
                                positionMs = curPos,
                                durationMs = dur,
                                sessionListenTimeMs = sessionListeningTimeMs
                            )
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _uiState.value = _uiState.value.copy(isBuffering = true)
                        }
                        Player.STATE_READY -> {
                            _uiState.value = _uiState.value.copy(
                                isBuffering = false,
                                durationMs = player.duration.coerceAtLeast(0L),
                                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                            )
                        }
                        Player.STATE_ENDED -> {
                            val endedSermon = _uiState.value.currentSermon
                            if (endedSermon != null) {
                                progressManager.clearProgress(endedSermon.id)
                                SermonResumeReminderManager.stopAlarm(context)
                            }
                            sessionListeningTimeMs = 0L
                            sessionPlayStartTime = 0L
                            _uiState.value = _uiState.value.copy(
                                isPlaying = false,
                                isBuffering = false,
                                currentPositionMs = 0L
                            )
                            playNext()
                        }
                        Player.STATE_IDLE -> {
                            _uiState.value = _uiState.value.copy(isBuffering = false)
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _uiState.value = _uiState.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = error.message ?: "Playback error"
                    )
                }
            })
        }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var resolveAndPlayJob: Job? = null
    private var sleepTimer: CountDownTimer? = null
    private var sessionPlayStartTime: Long = 0L
    private var sessionListeningTimeMs: Long = 0L

    init {
        // Player starts idle and closed until user explicitly plays a sermon
    }

    fun setupInitialMediaItem(sermon: Sermon, positionMs: Long = 0L) {
        if (player.mediaItemCount > 0) return
        val metadata = MediaMetadata.Builder()
            .setTitle(sermon.title)
            .setArtist(sermon.thero)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(sermon.id)
            .setUri(sermon.playbackUrl ?: "https://www.jethavanarama.org")
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        if (positionMs > 0L) {
            player.seekTo(positionMs)
        }
        player.playWhenReady = false
    }

    fun setupInitialSermon(sermon: Sermon) {
        if (_uiState.value.currentSermon == null) {
            val savedPos = progressManager.getSavedPosition(sermon.id)
            val savedDur = progressManager.getSavedDuration(sermon.id)
            _uiState.value = _uiState.value.copy(
                currentSermon = sermon.copy(lastPlayedPositionMs = savedPos),
                currentPositionMs = savedPos,
                durationMs = savedDur,
                isPlaying = false,
                queue = listOf(sermon)
            )
            setupInitialMediaItem(sermon, savedPos)
            ensureServiceStarted()
        }
    }

    fun ensureServiceStarted() {
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSermon(sermon: Sermon, newQueue: List<Sermon> = emptyList()) {
        // Save position of current playing sermon before switching
        saveCurrentPositionNow()

        if (_uiState.value.currentSermon?.id != sermon.id) {
            sessionListeningTimeMs = 0L
            sessionPlayStartTime = 0L
        }

        // 1. Immediately cancel any pending resolution
        resolveAndPlayJob?.cancel()

        // 2. Immediately stop previous audio playback and clear player items
        player.stop()
        player.clearMediaItems()

        // 3. Lookup saved resume position for this sermon
        val resumePositionMs = progressManager.getSavedPosition(sermon.id)
        val savedDurationMs = progressManager.getSavedDuration(sermon.id)

        val currentQueue = if (newQueue.isNotEmpty()) newQueue else {
            if (_uiState.value.queue.contains(sermon)) _uiState.value.queue else listOf(sermon)
        }

        _uiState.value = _uiState.value.copy(
            currentSermon = sermon.copy(lastPlayedPositionMs = resumePositionMs),
            queue = currentQueue,
            isPlaying = false,
            isBuffering = true,
            currentPositionMs = resumePositionMs,
            durationMs = savedDurationMs,
            bufferedPositionMs = 0L,
            errorMessage = null
        )

        // 4. Start PlaybackService normally
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Resolve stream URL and begin playback
        resolveAndPlayJob = scope.launch(Dispatchers.IO) {
            var playableUrl = sermon.playbackUrl
            var resolvedSermon = sermon

            if (playableUrl.isNullOrBlank()) {
                if (sermon.source == SermonSource.JETHAVANARAMA) {
                    resolvedSermon = jethavanaramaExtractor.resolveSermonAudioUrl(sermon)
                    playableUrl = resolvedSermon.playbackUrl

                    if (playableUrl.isNullOrBlank() && !resolvedSermon.youtubeVideoId.isNullOrBlank()) {
                        when (val ytRes = pahuraExtractor.extractAudioStreamUrl(resolvedSermon.youtubeVideoId)) {
                            is AudioExtractionResult.Success -> {
                                playableUrl = ytRes.audioUrl
                                resolvedSermon = resolvedSermon.copy(
                                    audioUrl = ytRes.audioUrl,
                                    durationSeconds = ytRes.durationSeconds
                                )
                            }
                            is AudioExtractionResult.Error -> {}
                        }
                    }
                } else if (sermon.source == SermonSource.PAHURA_YOUTUBE && !sermon.youtubeVideoId.isNullOrBlank()) {
                    when (val result = pahuraExtractor.extractAudioStreamUrl(sermon.youtubeVideoId)) {
                        is AudioExtractionResult.Success -> {
                            playableUrl = result.audioUrl
                            resolvedSermon = sermon.copy(
                                audioUrl = result.audioUrl,
                                durationSeconds = result.durationSeconds
                            )
                        }
                        is AudioExtractionResult.Error -> {
                            launch(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    isBuffering = false,
                                    errorMessage = result.message
                                )
                            }
                            return@launch
                        }
                    }
                }
            }

            if (playableUrl.isNullOrBlank()) {
                launch(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isBuffering = false,
                        errorMessage = "Unable to stream sermon"
                    )
                }
                return@launch
            }

            // Download thumbnail bytes safely for notification artwork
            var artworkBytes: ByteArray? = null
            if (!resolvedSermon.thumbnailUrl.isNullOrBlank()) {
                try {
                    val imgReq = Request.Builder().url(resolvedSermon.thumbnailUrl).build()
                    val imgResp = httpClient.newCall(imgReq).execute()
                    if (imgResp.isSuccessful) {
                        artworkBytes = imgResp.body?.bytes()
                    }
                } catch (e: Exception) {
                    // Non-fatal, artwork can be omitted
                }
            }

            launch(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    currentSermon = resolvedSermon.copy(lastPlayedPositionMs = resumePositionMs)
                )
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(resolvedSermon.title)
                    .setArtist(resolvedSermon.thero)

                if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                    metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }

                val metadata = metadataBuilder.build()

                val mediaItem = MediaItem.Builder()
                    .setUri(playableUrl)
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)

                // Seek directly to remembered position before prepare() to buffer right from where user left off
                if (resumePositionMs > 3000L) {
                    player.seekTo(resumePositionMs)
                }

                player.prepare()
                player.playWhenReady = true
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            saveCurrentPositionNow()
            player.pause()
        } else {
            if ((player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0) && _uiState.value.currentSermon != null) {
                playSermon(_uiState.value.currentSermon!!)
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(currentPositionMs = target)
        saveCurrentPositionNow()
    }

    fun seekRelative(offsetMs: Long) {
        val target = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(currentPositionMs = target)
        saveCurrentPositionNow()
    }

    fun setPlaybackSpeed(speed: Float) {
        val params = PlaybackParameters(speed)
        player.playbackParameters = params
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes <= 0) {
            _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = null, sleepTimerSecondsRemaining = 0L)
            return
        }

        val totalMs = minutes * 60 * 1000L
        _uiState.value = _uiState.value.copy(
            sleepTimerMinutesLeft = minutes,
            sleepTimerSecondsRemaining = totalMs / 1000
        )

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.value = _uiState.value.copy(
                    sleepTimerSecondsRemaining = millisUntilFinished / 1000,
                    sleepTimerMinutesLeft = (millisUntilFinished / 60000).toInt() + 1
                )
            }

            override fun onFinish() {
                saveCurrentPositionNow()
                player.pause()
                _uiState.value = _uiState.value.copy(
                    sleepTimerMinutesLeft = null,
                    sleepTimerSecondsRemaining = 0L
                )
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = null, sleepTimerSecondsRemaining = 0L)
    }

    fun playNext() {
        saveCurrentPositionNow()
        val current = _uiState.value.currentSermon ?: return
        val queue = _uiState.value.queue
        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < queue.size) {
            playSermon(queue[currentIndex + 1], queue)
        }
    }

    fun playPrevious() {
        saveCurrentPositionNow()
        val current = _uiState.value.currentSermon ?: return
        val queue = _uiState.value.queue
        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSermon(queue[currentIndex - 1], queue)
        } else {
            seekTo(0)
        }
    }

    /**
     * Instantly saves current playback progress to persistent storage.
     */
    fun saveCurrentPositionNow() {
        val sermon = _uiState.value.currentSermon ?: return
        val cur = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        if (cur > 0L) {
            progressManager.saveProgress(sermon, cur, dur)
        }
    }

    /**
     * Dismisses the floating mini player, stops playback and cleans up current sermon.
     */
    fun dismissMiniPlayer() {
        if (sessionPlayStartTime > 0L) {
            sessionListeningTimeMs += (System.currentTimeMillis() - sessionPlayStartTime)
            sessionPlayStartTime = 0L
        }
        val curSermon = _uiState.value.currentSermon
        val curPos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        if (curSermon != null) {
            SermonResumeReminderManager.checkAndScheduleOnStop(
                context = context,
                sermon = curSermon,
                positionMs = curPos,
                durationMs = dur,
                sessionListenTimeMs = sessionListeningTimeMs
            )
        }
        saveCurrentPositionNow()
        player.pause()
        player.stop()
        player.clearMediaItems()
        stopProgressTracker()
        _uiState.value = _uiState.value.copy(
            currentSermon = null,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 0L
        )
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val cur = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                val buf = player.bufferedPosition.coerceAtLeast(0L)

                _uiState.value = _uiState.value.copy(
                    currentPositionMs = cur,
                    durationMs = dur,
                    bufferedPositionMs = buf
                )

                // Save to persistent storage every 1.5 seconds continuously
                val sermon = _uiState.value.currentSermon
                if (sermon != null && cur > 1000L) {
                    progressManager.saveProgress(sermon, cur, dur)
                }

                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
