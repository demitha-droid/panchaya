package org.jethavanarama.dhammasermons.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jethavanarama.dhammasermons.MainActivity
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.model.Sermon
import java.util.concurrent.TimeUnit

object NewSermonNotificationManager {

    const val CHANNEL_ID = "new_sermons_channel"
    const val PREFS_NAME = "new_sermon_notifications_pref"
    const val KEY_SEEN_SERMON_IDS = "seen_sermon_ids"
    const val KEY_IS_INITIAL_RUN = "is_initial_run"
    const val WORK_TAG = "NEW_SERMON_CHECK_WORK"

    const val EXTRA_SERMON_ID = "extra_sermon_id"
    const val EXTRA_SERMON_TITLE = "extra_sermon_title"
    const val EXTRA_AUTO_PLAY = "extra_auto_play"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "නව ධර්ම දේශනා (New Sermons)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "නව ධර්ම දේශනාවක් නිකුත් වූ විට ක්ෂණිකව දැනුම්දීම් (Notifications when new sermons release)"
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun schedulePeriodicCheck(context: Context) {
        createNotificationChannel(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<NewSermonCheckWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun getSeenSermonIds(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SEEN_SERMON_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun saveSeenSermonIds(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_SEEN_SERMON_IDS, ids)
            .putBoolean(KEY_IS_INITIAL_RUN, false)
            .apply()
    }

    fun isInitialRun(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_INITIAL_RUN, true)
    }

    suspend fun showNewSermonNotification(context: Context, sermon: Sermon, notificationId: Int = (sermon.id.hashCode() and 0x7FFFFFFF)) {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SERMON_ID, sermon.id)
            putExtra(EXTRA_SERMON_TITLE, sermon.title)
            putExtra(EXTRA_AUTO_PLAY, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Try downloading thumbnail image for rich big picture style
        val thumbnailBitmap = withContext(Dispatchers.IO) {
            try {
                if (!sermon.thumbnailUrl.isNullOrBlank()) {
                    val req = Request.Builder().url(sermon.thumbnailUrl).build()
                    val response = okHttpClient.newCall(req).execute()
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }

        val notificationTitle = "නව ධර්ම දේශනාවක් එක්විය"
        val notificationBody = "${sermon.title}\n${sermon.thero.ifBlank { "ජෙතවනාරාම විහාර භූමිය" }}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dhamma_chakra)
            .setColor(0xFFFF9800.toInt()) // Saffron Gold accent
            .setContentTitle(notificationTitle)
            .setContentText(sermon.title)
            .setSubText(sermon.thero.ifBlank { "ජෙතවනාරාමය" })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (thumbnailBitmap != null) {
            builder.setLargeIcon(thumbnailBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnailBitmap)
                    .setBigContentTitle(notificationTitle)
                    .setSummaryText(notificationBody)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationTitle)
                    .bigText(notificationBody)
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())
    }
}
