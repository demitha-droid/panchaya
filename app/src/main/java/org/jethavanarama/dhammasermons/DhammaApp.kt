package org.jethavanarama.dhammasermons

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import org.jethavanarama.dhammasermons.data.downloader.SermonDownloadManager
import org.jethavanarama.dhammasermons.data.extractor.NewPipeDownloaderImpl
import org.jethavanarama.dhammasermons.player.AudioPlayerManager
import org.jethavanarama.dhammasermons.player.PlaybackService
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.concurrent.TimeUnit

@UnstableApi
class DhammaApp : Application(), ImageLoaderFactory {

    lateinit var playerManager: AudioPlayerManager
        private set

    lateinit var downloadManager: SermonDownloadManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Create playback notification channel early
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PlaybackService.NOTIFICATION_CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification controls for playing Dhamma sermons"
                setShowBadge(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // 2. Initialize NewPipeExtractor
        try {
            NewPipe.init(
                NewPipeDownloaderImpl.init(),
                Localization.DEFAULT,
                ContentCountry("US")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        playerManager = AudioPlayerManager.getInstance(this)
        downloadManager = SermonDownloadManager(this)

        // 3. Schedule background periodic worker for new sermon release notifications
        try {
            org.jethavanarama.dhammasermons.notifications.NewSermonNotificationManager.schedulePeriodicCheck(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
