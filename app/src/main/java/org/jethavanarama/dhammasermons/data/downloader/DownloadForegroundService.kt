package org.jethavanarama.dhammasermons.data.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jethavanarama.dhammasermons.DhammaApp
import org.jethavanarama.dhammasermons.MainActivity
import org.jethavanarama.dhammasermons.R
import java.util.Locale

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationManager: NotificationManager? = null

    companion object {
        const val CHANNEL_ID = "dhamma_downloads_channel"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()

        // Acquire WakeLock to keep CPU running during screen-off/lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DhammaSermons:DownloadWakeLock")?.apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // 1 hour max safety timeout
        }

        // Immediately start foreground with initial notification to satisfy Android 5s requirement
        val initialNotification = buildNotification("බාගත කිරීම් සූදානම් වෙමින් පවතී...", 0f, 0L, 0L)
        startForeground(NOTIFICATION_ID, initialNotification)

        // Observe active downloads from SermonDownloadManager
        val app = application as? DhammaApp
        val downloadManager = app?.downloadManager

        if (downloadManager != null) {
            serviceScope.launch {
                downloadManager.activeDownloads.collectLatest { activeList ->
                    if (activeList.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        val activeItem = activeList.firstOrNull { !it.isFailed } ?: activeList.first()
                        val notification = buildNotification(
                            title = activeItem.sermon.title,
                            progress = activeItem.progress,
                            downloadedBytes = activeItem.downloadedBytes,
                            totalBytes = activeItem.totalBytes
                        )
                        notificationManager?.notify(NOTIFICATION_ID, notification)
                    }
                }
            }
        }
    }

    private fun buildNotification(
        title: String,
        progress: Float,
        downloadedBytes: Long,
        totalBytes: Long
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val downloadedMb = downloadedBytes / (1024.0 * 1024.0)
        val totalMb = totalBytes / (1024.0 * 1024.0)
        val contentText = if (totalBytes > 0) {
            val percentage = (progress * 100).toInt()
            String.format(Locale.getDefault(), "%d%% • %.1f MB / %.1f MB", percentage, downloadedMb, totalMb)
        } else if (downloadedBytes > 0) {
            String.format(Locale.getDefault(), "%.1f MB බාගත විය (බාගත වෙමින්...)", downloadedMb)
        } else {
            "බාගත කිරීම ආරම්භ වෙමින් පවතී..."
        }

        val isIndeterminate = totalBytes <= 0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("බාගත වෙමින් පවතී: $title")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_dhamma_chakra)
            .setColor(0xFFFF9800.toInt())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, (progress * 100).toInt(), isIndeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "බාගත කිරීම් (Sermon Downloads)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress when phone is locked or app is minimized"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
