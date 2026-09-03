package org.jethavanarama.dhammasermons.data.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jethavanarama.dhammasermons.data.db.AppDatabase
import org.jethavanarama.dhammasermons.data.db.SermonEntity
import org.jethavanarama.dhammasermons.data.extractor.AudioExtractionResult
import org.jethavanarama.dhammasermons.data.extractor.JethavanaramaExtractor
import org.jethavanarama.dhammasermons.data.extractor.PahuraYouTubeExtractor
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ActiveDownloadItem(
    val sermon: Sermon,
    val progress: Float, // 0.0f to 1.0f
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isFailed: Boolean = false,
    val canResume: Boolean = false,
    val errorMessage: String? = null
)

class SermonDownloadManager(
    private val context: Context,
    private val jethavanaramaExtractor: JethavanaramaExtractor = JethavanaramaExtractor(),
    private val pahuraExtractor: PahuraYouTubeExtractor = PahuraYouTubeExtractor()
) {

    private val db = AppDatabase.getDatabase(context)
    private val sermonDao = db.sermonDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<ActiveDownloadItem>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownloadItem>> = _activeDownloads.asStateFlow()

    private val activeDownloadStates = ConcurrentHashMap<String, DownloadState>()
    private val activeDownloadMap = ConcurrentHashMap<String, ActiveDownloadItem>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val tempFiles = ConcurrentHashMap<String, File>()

    private val sermonsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), "dhamma_sermons")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun getDownloadState(sermonId: String): DownloadState {
        return activeDownloadStates[sermonId] ?: DownloadState.NotDownloaded
    }

    fun startDownload(sermon: Sermon) {
        if (activeDownloadStates[sermon.id] is DownloadState.Downloading) {
            return
        }

        val initialItem = ActiveDownloadItem(
            sermon = sermon,
            progress = 0.0f
        )
        activeDownloadMap[sermon.id] = initialItem
        updateActiveDownloadsList()
        updateState(sermon.id, DownloadState.Downloading(0.0f))

        // Start Foreground Service so download continues uninterrupted when screen turns off / phone locks
        try {
            DownloadForegroundService.start(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val job = scope.launch {
            var outputFile: File? = null
            try {
                // 1. Resolve audio URL if needed
                var resolvedAudioUrl = sermon.audioUrl
                var resolvedSermon = sermon

                if (resolvedAudioUrl.isNullOrBlank()) {
                    if (sermon.source == SermonSource.JETHAVANARAMA) {
                        resolvedSermon = jethavanaramaExtractor.resolveSermonAudioUrl(sermon)
                        resolvedAudioUrl = resolvedSermon.audioUrl

                        if (resolvedAudioUrl.isNullOrBlank() && !resolvedSermon.youtubeVideoId.isNullOrBlank()) {
                            when (val ytRes = pahuraExtractor.extractAudioStreamUrl(resolvedSermon.youtubeVideoId)) {
                                is AudioExtractionResult.Success -> {
                                    resolvedAudioUrl = ytRes.audioUrl
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
                                resolvedAudioUrl = result.audioUrl
                                resolvedSermon = sermon.copy(
                                    audioUrl = result.audioUrl,
                                    durationSeconds = result.durationSeconds
                                )
                            }
                            is AudioExtractionResult.Error -> {
                                markFailed(sermon, result.message, canResume = false)
                                return@launch
                            }
                        }
                    }
                }

                if (resolvedAudioUrl.isNullOrBlank()) {
                    markFailed(sermon, "Unable to resolve audio download stream", canResume = false)
                    return@launch
                }

                // 2. Prepare output file and check existing partial progress for resume
                val extension = if (resolvedAudioUrl.contains(".m4a") || resolvedAudioUrl.contains("audio/mp4")) "m4a" else "mp3"
                val safeId = sermon.id.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val safeTitle = resolvedSermon.title.replace("[^a-zA-Z0-9\\u0D80-\\u0DFF]".toRegex(), "_").take(30)
                val fileName = "sermon_${safeId}_${safeTitle}.$extension"
                outputFile = File(sermonsDir, fileName)
                tempFiles[sermon.id] = outputFile

                val existingBytes = if (outputFile.exists()) outputFile.length() else 0L

                val requestBuilder = Request.Builder()
                    .url(resolvedAudioUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "*/*")

                // Send HTTP Range header if resuming partial file
                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val request = requestBuilder.build()
                val call = client.newCall(request)
                activeCalls[sermon.id] = call

                val response = call.execute()

                // If range request was 416 (Range Not Satisfiable), restart from 0
                if (response.code == 416) {
                    outputFile.delete()
                    startDownload(sermon)
                    return@launch
                }

                if (!response.isSuccessful && response.code != 206) {
                    markFailed(sermon, "HTTP ${response.code} ${response.message}", canResume = existingBytes > 0L)
                    return@launch
                }

                val isPartial = response.code == 206
                val append = isPartial && existingBytes > 0L
                val body = response.body ?: throw IllegalStateException("Empty response body from server")
                val streamContentLength = body.contentLength()
                val totalBytes = if (isPartial && streamContentLength > 0) {
                    existingBytes + streamContentLength
                } else {
                    streamContentLength
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile, append)

                val buffer = ByteArray(8 * 1024)
                var bytesRead = 0
                var totalBytesRead = if (append) existingBytes else 0L
                var lastProgressReportTime = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (coroutineContext.isActive && input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressReportTime > 200 || (totalBytes > 0 && totalBytesRead == totalBytes)) {
                                lastProgressReportTime = now
                                val progress = if (totalBytes > 0) {
                                    (totalBytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                activeDownloadMap[sermon.id] = ActiveDownloadItem(
                                    sermon = resolvedSermon,
                                    progress = progress,
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = totalBytes
                                )
                                updateActiveDownloadsList()
                                updateState(sermon.id, DownloadState.Downloading(progress))
                            }
                        }
                    }
                }

                if (!coroutineContext.isActive) {
                    return@launch
                }

                // 3. Save copy of the MP3 to the public system Downloads/DhammaSermons folder if enabled (default true)
                val shouldSavePublicMp3 = context.getSharedPreferences("panchaya_settings", Context.MODE_PRIVATE)
                    .getBoolean("save_downloads_as_mp3", true)
                if (shouldSavePublicMp3) {
                    saveToPublicDownloadsFolder(outputFile, resolvedSermon)
                }

                // 4. Mark as downloaded in Room DB
                val updatedSermon = resolvedSermon.copy(
                    localFilePath = outputFile.absolutePath,
                    downloadState = DownloadState.Downloaded(outputFile.absolutePath)
                )
                sermonDao.insertOrUpdate(SermonEntity.fromDomain(updatedSermon, System.currentTimeMillis()))

                tempFiles.remove(sermon.id)
                activeCalls.remove(sermon.id)
                activeDownloadMap.remove(sermon.id)
                updateActiveDownloadsList()
                updateState(sermon.id, DownloadState.Downloaded(outputFile.absolutePath))

            } catch (e: Exception) {
                if (e is IOException && e.message?.contains("Canceled", ignoreCase = true) == true) {
                    // Intentionally cancelled by user
                } else {
                    e.printStackTrace()
                    val existing = outputFile?.length() ?: 0L
                    markFailed(
                        sermon = sermon,
                        error = if (existing > 0) "බාගත කිරීම ඇනහිට ඇත (Connection lost). නැවත අරඹන්න (Resume)." else (e.message ?: "Download failed"),
                        canResume = existing > 0
                    )
                }
            } finally {
                downloadJobs.remove(sermon.id)
                activeCalls.remove(sermon.id)
                if (activeDownloadMap.isEmpty()) {
                    try {
                        DownloadForegroundService.stop(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        downloadJobs[sermon.id] = job
    }

    fun resumeDownload(sermon: Sermon) {
        startDownload(sermon)
    }

    fun cancelDownload(sermonId: String) {
        activeCalls[sermonId]?.cancel()
        activeCalls.remove(sermonId)

        downloadJobs[sermonId]?.cancel()
        downloadJobs.remove(sermonId)

        tempFiles[sermonId]?.let { file ->
            try {
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tempFiles.remove(sermonId)

        activeDownloadMap.remove(sermonId)
        updateActiveDownloadsList()
        updateState(sermonId, DownloadState.NotDownloaded)

        if (activeDownloadMap.isEmpty()) {
            try {
                DownloadForegroundService.stop(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun markFailed(sermon: Sermon, error: String, canResume: Boolean) {
        val existing = tempFiles[sermon.id]?.length() ?: 0L
        val item = ActiveDownloadItem(
            sermon = sermon,
            progress = if (itemHasProgress(sermon.id)) activeDownloadMap[sermon.id]!!.progress else 0f,
            downloadedBytes = existing,
            isFailed = true,
            canResume = canResume,
            errorMessage = error
        )
        activeDownloadMap[sermon.id] = item
        updateActiveDownloadsList()
        updateState(sermon.id, DownloadState.Failed(error, canResume))

        if (activeDownloadMap.values.none { !it.isFailed }) {
            try {
                DownloadForegroundService.stop(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun itemHasProgress(sermonId: String): Boolean {
        return activeDownloadMap[sermonId]?.downloadedBytes ?: 0L > 0L
    }

    fun deleteDownload(sermon: Sermon) {
        scope.launch {
            try {
                sermon.localFilePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                sermonDao.removeDownload(sermon.id)
                updateState(sermon.id, DownloadState.NotDownloaded)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getStorageUsedBytes(): Long = withContext(Dispatchers.IO) {
        try {
            sermonsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            0L
        }
    }

    private fun updateState(sermonId: String, state: DownloadState) {
        activeDownloadStates[sermonId] = state
        _downloadStates.value = activeDownloadStates.toMap()
    }

    private fun updateActiveDownloadsList() {
        _activeDownloads.value = activeDownloadMap.values.toList()
    }

    /**
     * Copies or exports the downloaded audio file to the public Downloads/DhammaSermons folder
     * using MediaStore (API 29+) or legacy public file storage (API <= 28).
     * This ensures the MP3 remains on the user's device even if the app is uninstalled.
     */
    private fun saveToPublicDownloadsFolder(sourceFile: File, sermon: Sermon) {
        try {
            val cleanTitle = sermon.title.replace("[\\\\/:*?\"<>|]".toRegex(), " ").trim().take(80)
            val cleanThero = sermon.thero.replace("[\\\\/:*?\"<>|]".toRegex(), " ").trim().take(40)
            val ext = if (sourceFile.name.endsWith(".m4a", ignoreCase = true)) "m4a" else "mp3"
            val mimeType = if (ext == "m4a") "audio/mp4" else "audio/mpeg"
            val publicFileName = if (cleanThero.isNotEmpty()) "$cleanTitle - $cleanThero.$ext" else "$cleanTitle.$ext"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, publicFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DhammaSermons")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)

                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }
            } else {
                // Android 9 and below
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DhammaSermons"
                )
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                val destFile = File(publicDir, publicFileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Notify media scanner
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
