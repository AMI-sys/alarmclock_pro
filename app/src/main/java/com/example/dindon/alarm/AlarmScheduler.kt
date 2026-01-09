package com.example.dindon.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val triggerAt = computeNextTriggerMillis(alarm)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pi = buildTriggerPendingIntent(
            context = context,
            alarmId = alarm.id,
            label = alarm.label,
            soundId = alarm.sound,
            vibrate = alarm.vibrate,
            snoozeMin = alarm.snoozeMinutes
        )

        setExactSafely(am, triggerAt, pi)
    }

    fun cancel(context: Context, alarmId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // основной pending intent
        val pi = buildTriggerPendingIntent(context, alarmId, "", "", vibrate = false, snoozeMin = 5)
        am.cancel(pi)

        // на всякий случай отменим и snooze pending intent (если был)
        val snoozePi = buildSnoozePendingIntent(
            context = context,
            alarmId = alarmId,
            label = "",
            soundId = "",
            vibrate = false,
            snoozeMin = 5,
            triggerAt = 0L
        )
        am.cancel(snoozePi)
    }

    /**
     * После ребута AlarmManager очищается — пересоздаем из сохраненных
     */
    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        // Чтобы не было дублей:
        alarms.forEach { cancel(context, it.id) }

        // И ставим заново (если у Alarm есть enabled/isEnabled — лучше фильтровать тут)
        alarms.forEach { schedule(context, it) }
    }

    fun snooze(context: Context, alarmId: Int, label: String, soundId: String, vibrate: Boolean, snoozeMin: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + snoozeMin * 60_000L

        val pi = buildSnoozePendingIntent(
            context = context,
            alarmId = alarmId,
            label = label,
            soundId = soundId,
            vibrate = vibrate,
            snoozeMin = snoozeMin,
            triggerAt = triggerAt
        )

        setExactSafely(am, triggerAt, pi)
    }

    /**
     * Android 12+ может требовать право на exact alarms.
     * Чтобы не падать и убрать предупреждения — делаем check + try/catch + fallback.
     */
    private fun setExactSafely(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun buildTriggerPendingIntent(
        context: Context,
        alarmId: Int,
        label: String,
        soundId: String,
        vibrate: Boolean,
        snoozeMin: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_TRIGGER
            putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActions.EXTRA_LABEL, label)
            putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
            putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
        }

        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePendingIntent(
        context: Context,
        alarmId: Int,
        label: String,
        soundId: String,
        vibrate: Boolean,
        snoozeMin: Int,
        triggerAt: Long
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_TRIGGER
            putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActions.EXTRA_LABEL, label)
            putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
            putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
            putExtra(AlarmActions.EXTRA_TRIGGER_AT, triggerAt)
        }

        val requestCode = alarmId + 1_000_000 // чтобы snooze не конфликтовал с основным alarmId
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTriggerMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()

        fun calendarAtToday(): Calendar =
            Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
            }

        val days = alarm.days
        if (days.isEmpty()) {
            val c = calendarAtToday()
            if (c.timeInMillis <= now.timeInMillis) c.add(Calendar.DAY_OF_YEAR, 1)
            return c.timeInMillis
        }

        var best: Calendar? = null
        for (d in days) {
            val c = calendarAtToday()
            val targetDow = toCalendarDow(d)
            val currentDow = c.get(Calendar.DAY_OF_WEEK)

            var addDays = (targetDow - currentDow + 7) % 7
            if (addDays == 0 && c.timeInMillis <= now.timeInMillis) addDays = 7
            c.add(Calendar.DAY_OF_YEAR, addDays)

            if (best == null || c.timeInMillis < best!!.timeInMillis) best = c
        }

        // days не пустой, но на всякий случай без !!
        return best?.timeInMillis ?: run {
            val c = calendarAtToday()
            if (c.timeInMillis <= now.timeInMillis) c.add(Calendar.DAY_OF_YEAR, 1)
            c.timeInMillis
        }
    }

    private fun toCalendarDow(d: WeekDay): Int = when (d) {
        WeekDay.Mon -> Calendar.MONDAY
        WeekDay.Tue -> Calendar.TUESDAY
        WeekDay.Wed -> Calendar.WEDNESDAY
        WeekDay.Thu -> Calendar.THURSDAY
        WeekDay.Fri -> Calendar.FRIDAY
        WeekDay.Sat -> Calendar.SATURDAY
        WeekDay.Sun -> Calendar.SUNDAY
    }
}
