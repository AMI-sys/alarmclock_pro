package com.example.dindon.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val alarmId = intent.getIntExtra(AlarmActions.EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(AlarmActions.EXTRA_LABEL) ?: "Alarm"
        val soundId = intent.getStringExtra(AlarmActions.EXTRA_SOUND_ID) ?: "clock"
        val vibrate = intent.getBooleanExtra(AlarmActions.EXTRA_VIBRATE, true)
        val snoozeMin = intent.getIntExtra(AlarmActions.EXTRA_SNOOZE_MIN, 5)

        when (action) {
            AlarmActions.ACTION_TRIGGER -> {
                // запускаем foreground service
                val s = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmActions.ACTION_TRIGGER
                    putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmActions.EXTRA_LABEL, label)
                    putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                    putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                    putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
                }
                context.startForegroundService(s)
            }

            AlarmActions.ACTION_DISMISS -> {
                val s = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmActions.ACTION_DISMISS
                }
                context.startForegroundService(s)
            }

            AlarmActions.ACTION_SNOOZE -> {
                val s = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmActions.ACTION_SNOOZE
                    putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmActions.EXTRA_LABEL, label)
                    putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                    putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                    putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
                }
                context.startForegroundService(s)
            }
        }
    }
}
