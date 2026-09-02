package org.jethavanarama.dhammasermons.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SermonResumeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            SermonResumeReminderManager.ACTION_SNOOZE -> {
                // Show alarm again in 6 hours
                SermonResumeReminderManager.snooze(context, hours = 6)
            }
            SermonResumeReminderManager.ACTION_STOP -> {
                // Stop and cancel future alarms
                SermonResumeReminderManager.stopAlarm(context)
            }
        }
    }
}
