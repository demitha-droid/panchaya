package org.jethavanarama.dhammasermons.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import org.jethavanarama.dhammasermons.MainActivity
import org.jethavanarama.dhammasermons.R

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var customNotificationProvider: CustomMediaNotificationProvider? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "dhamma_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "org.jethavanarama.dhammasermons.ACTION_PLAY"
        const val ACTION_PAUSE = "org.jethavanarama.dhammasermons.ACTION_PAUSE"
        const val ACTION_NEXT = "org.jethavanarama.dhammasermons.ACTION_NEXT"
        const val ACTION_PREV = "org.jethavanarama.dhammasermons.ACTION_PREV"
        const val ACTION_STOP = "org.jethavanarama.dhammasermons.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Create notification channel with VISIBILITY_PUBLIC for lock screen display
        createNotificationChannel()

        // 2. Build MediaSession with pending activity intent
        val player = AudioPlayerManager.getInstance(applicationContext).player
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        // 3. Attach CustomMediaNotificationProvider for rich Lock Screen & Notification Mini Player
        val provider = CustomMediaNotificationProvider(this)
        customNotificationProvider = provider
        setMediaNotificationProvider(provider)

        // 4. Start foreground notification immediately on service start
        try {
            val notif = provider.buildNotification(mediaSession!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = AudioPlayerManager.getInstance(applicationContext).player
        val playerManager = AudioPlayerManager.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_PLAY -> {
                if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0 || !player.isPlaying) {
                    playerManager.togglePlayPause()
                } else {
                    player.play()
                }
            }
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> playerManager.playNext()
            ACTION_PREV -> playerManager.playPrevious()
            ACTION_STOP -> {
                player.stop()
                stopSelf()
            }
        }

        // Keep foreground notification updated
        try {
            if (mediaSession != null && customNotificationProvider != null) {
                val notif = customNotificationProvider!!.buildNotification(mediaSession!!)
                val notifManager = getSystemService(NotificationManager::class.java)
                notifManager?.notify(NOTIFICATION_ID, notif)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ධර්ම දේශනා වාදකය (Dhamma Sermons Playback Controls)"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            AudioPlayerManager.getInstance(applicationContext).saveCurrentPositionNow()
        } catch (e: Exception) {
            // Non-fatal
        }
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            AudioPlayerManager.getInstance(applicationContext).saveCurrentPositionNow()
        } catch (e: Exception) {
            // Non-fatal
        }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

@UnstableApi
class CustomMediaNotificationProvider(private val context: Context) : MediaNotification.Provider {

    fun buildNotification(mediaSession: MediaSession): Notification {
        val player = mediaSession.player
        val currentMediaItem = player.currentMediaItem
        val metadata = currentMediaItem?.mediaMetadata
        val playerManager = AudioPlayerManager.getInstance(context)
        val curSermon = playerManager.uiState.value.currentSermon

        val title = metadata?.title?.toString()
            ?: curSermon?.title
            ?: "ධර්ම දේශනා (Dhamma Sermon)"

        val artist = metadata?.artist?.toString()
            ?: curSermon?.thero
            ?: "ජෙතවනාරාමය & පහුර"

        val isPlaying = player.isPlaying

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Previous intent
        val prevIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            context,
            1,
            prevIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Play/Pause intent
        val playPauseIntent = Intent(context, PlaybackService::class.java).apply {
            action = if (isPlaying) PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            context,
            2,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Next intent
        val nextIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context,
            3,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, PlaybackService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_dhamma_chakra)
            .setColor(0xFFFF9800.toInt())
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Large icon artwork
        var hasLargeIcon = false
        metadata?.artworkData?.let { bytes ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    hasLargeIcon = true
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        if (!hasLargeIcon) {
            try {
                val defaultBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                if (defaultBitmap != null) {
                    builder.setLargeIcon(defaultBitmap)
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        // Add Previous, Play/Pause, Next actions
        builder.addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        )
        builder.addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)

        // Apply MediaStyle to render the rich mini player on Lock Screen and Notification Shade
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        builder.setStyle(mediaStyle)

        return builder.build()
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val notification = buildNotification(mediaSession)
        return MediaNotification(PlaybackService.NOTIFICATION_ID, notification)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean = false
}
