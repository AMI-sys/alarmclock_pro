package com.example.dindon.alarm

object AlarmActions {
    const val ACTION_TRIGGER = "com.example.dindon.alarm.ACTION_TRIGGER"
    const val ACTION_DISMISS = "com.example.dindon.alarm.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.example.dindon.alarm.ACTION_SNOOZE"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_LABEL = "extra_label"
    const val EXTRA_SOUND_ID = "extra_sound_id"
    const val EXTRA_VIBRATE = "extra_vibrate"
    const val EXTRA_SNOOZE_MIN = "extra_snooze_min"
    const val EXTRA_TRIGGER_AT = "extra_trigger_at" // для snooze one-shot
}
