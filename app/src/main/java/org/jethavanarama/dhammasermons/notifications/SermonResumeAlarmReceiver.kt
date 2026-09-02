package org.jethavanarama.dhammasermons.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SermonResumeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == SermonResumeReminderManager.ACTION_ALARM_TRIGGER) {
            val isReminderActive = SermonResumeReminderManager.isReminderActive(context)
            val sermon = SermonResumeReminderManager.getActiveSermon(context)
            val posMs = SermonResumeReminderManager.getActivePositionMs(context)
            val durMs = SermonResumeReminderManager.getActiveDurationMs(context)

            if (isReminderActive && sermon != null) {
                // Show notification with full-screen intent and actionable buttons
                SermonResumeReminderManager.showResumeNotificationAndFullScreen(
                    context = context,
                    sermon = sermon,
                    positionMs = posMs,
                    durationMs = durMs
                )

                // Schedule next 6-hour alarm so it repeats every 6 hours until resumed or stopped
                SermonResumeReminderManager.scheduleAlarm(
                    context = context,
                    delayMs = SermonResumeReminderManager.SIX_HOURS_MS
                )
            }
        }
    }
}
