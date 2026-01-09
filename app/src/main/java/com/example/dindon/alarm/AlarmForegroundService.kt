package com.example.dindon.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.dindon.R
import com.example.dindon.ui.sound.AlarmSounds

class AlarmForegroundService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            AlarmActions.ACTION_TRIGGER -> {
                val alarmId = intent.getIntExtra(AlarmActions.EXTRA_ALARM_ID, -1)
                val label = intent.getStringExtra(AlarmActions.EXTRA_LABEL) ?: "Alarm"
                val soundId = intent.getStringExtra(AlarmActions.EXTRA_SOUND_ID) ?: "american"
                val vibrate = intent.getBooleanExtra(AlarmActions.EXTRA_VIBRATE, true)
                val snoozeMin = intent.getIntExtra(AlarmActions.EXTRA_SNOOZE_MIN, 5)

                acquireWakeLock()
                startForeground(
                    NOTIF_ID,
                    buildNotification(alarmId, label, soundId, vibrate, snoozeMin)
                )
                startRing(soundId)

                startActivity(
                    Intent(this, AlarmRingActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                        putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                        putExtra(AlarmActions.EXTRA_LABEL, label)
                        putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                        putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                        putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
                    }
                )
            }

            AlarmActions.ACTION_DISMISS -> {
                stopEverything()
                stopSelf()
            }

            AlarmActions.ACTION_SNOOZE -> {
                val alarmId = intent.getIntExtra(AlarmActions.EXTRA_ALARM_ID, -1)
                val label = intent.getStringExtra(AlarmActions.EXTRA_LABEL) ?: "Alarm"
                val soundId = intent.getStringExtra(AlarmActions.EXTRA_SOUND_ID) ?: "american"
                val vibrate = intent.getBooleanExtra(AlarmActions.EXTRA_VIBRATE, true)
                val snoozeMin = intent.getIntExtra(AlarmActions.EXTRA_SNOOZE_MIN, 5)

                stopEverything()
                AlarmScheduler.snooze(this, alarmId, label, soundId, vibrate, snoozeMin)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    private fun buildNotification(
        alarmId: Int,
        label: String,
        soundId: String,
        vibrate: Boolean,
        snoozeMin: Int
    ): Notification {
        createChannel()

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmRingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmActions.EXTRA_LABEL, label)
                putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPi = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmActions.ACTION_DISMISS
                putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePi = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmActions.ACTION_SNOOZE
                putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmActions.EXTRA_LABEL, label)
                putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Будильник")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "Snooze", snoozePi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
    }

    private fun startRing(soundId: String) {
        stopRing()

        // Твой готовый метод, который всегда вернёт валидный звук (или первый в списке)
        val sound = AlarmSounds.byId(soundId)
        val resId = sound.resId

        val mp = MediaPlayer.create(this, resId) ?: return
        player = mp

        mp.isLooping = true
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.start()
    }

    private fun stopRing() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
    }

    private fun stopEverything() {
        stopRing()
        releaseWakeLock()
        stopForeground(true)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "dindon:alarm_wakelock"
        ).apply {
            acquire(10 * 60 * 1000L) // максимум 10 минут
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIF_ID = 1001
    }
}
