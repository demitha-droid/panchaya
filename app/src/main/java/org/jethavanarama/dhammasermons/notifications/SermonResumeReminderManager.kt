package org.jethavanarama.dhammasermons.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import org.jethavanarama.dhammasermons.MainActivity
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.ui.i18n.AppLanguage
import org.jethavanarama.dhammasermons.ui.i18n.getStrings

object SermonResumeReminderManager {

    const val CHANNEL_ID = "sermon_resume_reminder_channel"
    const val NOTIFICATION_ID = 2001
    const val PREFS_NAME = "dhamma_resume_reminder_prefs"
    const val KEY_SERMON_JSON = "active_sermon_json"
    const val KEY_POSITION_MS = "active_position_ms"
    const val KEY_DURATION_MS = "active_duration_ms"
    const val KEY_IS_ACTIVE = "is_reminder_active"
    const val KEY_LAST_SCHEDULED_TIME = "last_scheduled_time"

    const val ONE_HOUR_MS = 60 * 60 * 1000L // 1 hour threshold
    const val SIX_HOURS_MS = 6 * 60 * 60 * 1000L // 6 hours recurrence

    const val ACTION_ALARM_TRIGGER = "org.jethavanarama.dhammasermons.ACTION_SERMON_RESUME_ALARM"
    const val ACTION_SNOOZE = "org.jethavanarama.dhammasermons.ACTION_SNOOZE_RESUME"
    const val ACTION_STOP = "org.jethavanarama.dhammasermons.ACTION_STOP_RESUME"

    private val gson = Gson()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "දේශනා නැවත ඇරඹීමේ මතක් කිරීම් (Resume Sermon Reminders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "පැය 1කට වඩා සවන් දී නැවැත්වූ දේශනා නැවත ඇරඹීම සඳහා මතක් කිරීම් (Full-screen reminders to resume sermons)"
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Called when user pauses, stops, or leaves a sermon.
     * If user listened for > 1 hour (or position >= 1 hr) and didn't complete it, schedule 6-hour alarm.
     */
    fun checkAndScheduleOnStop(
        context: Context,
        sermon: Sermon,
        positionMs: Long,
        durationMs: Long,
        sessionListenTimeMs: Long
    ) {
        if (sermon.id.isBlank()) return

        // If completed or near completion (within 10s or 98%), stop any reminder
        val isCompleted = (durationMs > 0 && positionMs >= durationMs - 10000L) ||
                (durationMs > 10000L && positionMs >= durationMs * 0.98f)
        if (isCompleted) {
            stopAlarm(context)
            return
        }

        // Trigger condition: User listened for > 1 hour OR current position is > 1 hour
        val hasListenedOverHour = positionMs >= ONE_HOUR_MS || sessionListenTimeMs >= ONE_HOUR_MS
        if (!hasListenedOverHour) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SERMON_JSON, gson.toJson(sermon.copy(lastPlayedPositionMs = positionMs)))
            .putLong(KEY_POSITION_MS, positionMs)
            .putLong(KEY_DURATION_MS, durationMs)
            .putBoolean(KEY_IS_ACTIVE, true)
            .putLong(KEY_LAST_SCHEDULED_TIME, System.currentTimeMillis())
            .apply()

        scheduleAlarm(context, SIX_HOURS_MS)
    }

    fun scheduleAlarm(context: Context, delayMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SermonResumeAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags)

        val triggerAtMillis = System.currentTimeMillis() + delayMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Snooze: Reschedules alarm for +6 hours and dismisses current notification.
     */
    fun snooze(context: Context, hours: Int = 6) {
        dismissNotification(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IS_ACTIVE, false)) {
            scheduleAlarm(context, hours * 60 * 60 * 1000L)
        }
    }

    /**
     * Stop Alarm: Cancels future alarms, marks reminder inactive, and dismisses notification.
     */
    fun stopAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, SermonResumeAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags)
        alarmManager?.cancel(pendingIntent)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, false)
            .apply()

        dismissNotification(context)
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    fun getActiveSermon(context: Context): Sermon? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SERMON_JSON, null) ?: return null
        return try {
            gson.fromJson(json, Sermon::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getActivePositionMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_POSITION_MS, 0L)
    }

    fun getActiveDurationMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_DURATION_MS, 0L)
    }

    fun isReminderActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ACTIVE, false)
    }

    fun showResumeNotificationAndFullScreen(context: Context, sermon: Sermon, positionMs: Long, durationMs: Long) {
        createNotificationChannel(context)

        val settingsPrefs = context.getSharedPreferences("panchaya_settings", Context.MODE_PRIVATE)
        val langName = settingsPrefs.getString("app_language", AppLanguage.SINHALA.name)
        val appLang = try { AppLanguage.valueOf(langName ?: AppLanguage.SINHALA.name) } catch (e: Exception) { AppLanguage.SINHALA }
        val str = appLang.getStrings()

        val immFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // 1. Full-Screen Intent (Opens full-screen reminder Activity)
        val fullScreenIntent = Intent(context, SermonResumeReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("sermon_id", sermon.id)
            putExtra("sermon_title", sermon.title)
            putExtra("position_ms", positionMs)
            putExtra("duration_ms", durationMs)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 2001, fullScreenIntent, immFlags)

        // 2. Action: Resume Listening (Launches MainActivity and auto-plays)
        val resumeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NewSermonNotificationManager.EXTRA_SERMON_ID, sermon.id)
            putExtra(NewSermonNotificationManager.EXTRA_AUTO_PLAY, true)
        }
        val resumePendingIntent = PendingIntent.getActivity(context, 2002, resumeIntent, immFlags)

        // 3. Action: Snooze / Remind in 6 Hours (Show alarm again)
        val snoozeIntent = Intent(context, SermonResumeActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, 2003, snoozeIntent, immFlags)

        // 4. Action: Stop Alarm
        val stopIntent = Intent(context, SermonResumeActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(context, 2004, stopIntent, immFlags)

        val posStr = formatDuration(positionMs)
        val durStr = if (durationMs > 0) formatDuration(durationMs) else ""
        val contentSubtitle = if (durStr.isNotEmpty()) "$posStr / $durStr • ${sermon.thero}" else sermon.thero

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dhamma_chakra)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle("⏰ ${str.resumeReminderTitle}")
            .setContentText("${sermon.title} ($contentSubtitle)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${sermon.title}\n${sermon.thero}\n${str.resumeReminderSubtitle} ($posStr / $durStr)")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_dhamma_chakra, "▶ ${str.resumeButton}", resumePendingIntent)
            .addAction(R.drawable.ic_dhamma_chakra, "⏰ ${str.remindIn6Hours}", snoozePendingIntent)
            .addAction(R.drawable.ic_dhamma_chakra, "⏹ ${str.stopAlarm}", stopPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)

        // Explicitly start the full-screen activity so it pops up immediately over any screen/lockscreen
        try {
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSec = millis / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
