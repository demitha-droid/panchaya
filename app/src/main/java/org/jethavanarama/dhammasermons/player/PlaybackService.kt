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
        const val ACTION_REWIND_10 = "org.jethavanarama.dhammasermons.ACTION_REWIND_10"
        const val ACTION_FORWARD_10 = "org.jethavanarama.dhammasermons.ACTION_FORWARD_10"
        const val ACTION_NEXT = "org.jethavanarama.dhammasermons.ACTION_NEXT"
        const val ACTION_PREV = "org.jethavanarama.dhammasermons.ACTION_PREV"
        const val ACTION_STOP = "org.jethavanarama.dhammasermons.ACTION_STOP"
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Create notification channel with VISIBILITY_PUBLIC for lock screen display
        createNotificationChannel()

        // 2. Build MediaSession with pending activity intent
        val player = AudioPlayerManager.getInstance(applicationContext).player
        player.addListener(playerListener)

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
            ACTION_REWIND_10 -> playerManager.seekBack10()
            ACTION_FORWARD_10 -> playerManager.seekForward10()
            ACTION_NEXT -> playerManager.playNext()
            ACTION_PREV -> playerManager.playPrevious()
            ACTION_STOP -> {
                player.stop()
                stopSelf()
            }
        }

        updateNotification()

        return super.onStartCommand(intent, flags, startId)
    }

    fun updateNotification() {
        try {
            val session = mediaSession ?: return
            val provider = customNotificationProvider ?: return
            val notif = provider.buildNotification(session)
            val notifManager = getSystemService(NotificationManager::class.java)
            notifManager?.notify(NOTIFICATION_ID, notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            AudioPlayerManager.getInstance(applicationContext).player.removeListener(playerListener)
        } catch (e: Exception) {
            // Non-fatal
        }
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

        val playbackSpeed = playerManager.uiState.value.playbackSpeed

        val title = metadata?.title?.toString()
            ?: curSermon?.title
            ?: "ධර්ම දේශනා (Dhamma Sermon)"

        val artist = metadata?.artist?.toString()
            ?: curSermon?.thero
            ?: "ජෙතවනාරාමය • ධර්ම දේශනා"

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

        // 10s Rewind intent
        val rewindIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_REWIND_10
        }
        val rewindPendingIntent = PendingIntent.getService(
            context,
            10,
            rewindIntent,
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

        // 10s Forward intent
        val forwardIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_FORWARD_10
        }
        val forwardPendingIntent = PendingIntent.getService(
            context,
            11,
            forwardIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Concept 1 (Sermon Pro) speed and status subtext badge
        val speedStr = if (playbackSpeed != 1.0f) "${playbackSpeed}x" else "1.0x"
        val subText = "$speedStr • ධර්ම ශ්‍රවණය"

        val builder = NotificationCompat.Builder(context, PlaybackService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_dhamma_chakra)
            .setColor(0xFFFF9800.toInt()) // Monastic warm saffron accent
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
                val defaultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.home_header_icon)
                    ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                if (defaultBitmap != null) {
                    builder.setLargeIcon(defaultBitmap)
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        // Concept 1 (Sermon Pro): 10s Back | Play/Pause | 10s Forward
        builder.addAction(R.drawable.ic_replay_10, "10s Back", rewindPendingIntent)
        builder.addAction(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        )
        builder.addAction(R.drawable.ic_forward_10, "10s Forward", forwardPendingIntent)

        // Apply MediaStyle to render the rich Sermon Pro mini player on Lock Screen & Notification Shade
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
    ): Boolean {
        val playerManager = AudioPlayerManager.getInstance(context)
        return when (action) {
            PlaybackService.ACTION_REWIND_10 -> {
                playerManager.seekBack10()
                true
            }
            PlaybackService.ACTION_FORWARD_10 -> {
                playerManager.seekForward10()
                true
            }
            else -> false
        }
    }
}
