package com.example.dindon.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dindon.data.AlarmStore

class AlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                val alarms = AlarmStore(context).getAlarms()
                AlarmScheduler.rescheduleAll(context, alarms)
            }
        }
    }
}
