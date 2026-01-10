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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.dindon.R
import com.example.dindon.ui.sound.AlarmSounds
import android.net.Uri


class AlarmForegroundService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var audioFocusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        handler.post {
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // вернули фокус — продолжаем
                    player?.let { mp ->
                        if (!mp.isPlaying) {
                            runCatching { mp.start() }
                            // если мы уже дошли до полной громкости — не делаем fade заново
                            if (!reachedFullVolume) startFadeIn() else mp.setVolume(1f, 1f)
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // кратковременная потеря (звонок/ассистент) — пауза
                    player?.runCatching { if (isPlaying) pause() }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // потеря фокуса — останавливаем звук
                    stopRing()
                }
            }
        }
    }

    private val fadeDurationMs = 10_000L
    private val fadeStepMs = 100L
    private var fadeRunnable: Runnable? = null
    private var reachedFullVolume = false
    private val handler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }


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
                if (vibrate) startVibration()

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
            .setFullScreenIntent(fullScreenIntent, shouldUseFullScreen())
            .addAction(0, "Snooze", snoozePi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
    }

    private fun startRing(soundId: String) {
        stopRing()
        reachedFullVolume = false
        requestAlarmAudioFocus()

        val sound = AlarmSounds.byId(soundId) ?: return

        val mp = MediaPlayer().apply {
            setAudioAttributes(alarmAudioAttributes())

            isLooping = true
            setVolume(0f, 0f) // start silent, fade-in after playback starts

            if (sound.resId != null) {
                // Built-in sound from raw resources
                val afd = resources.openRawResourceFd(sound.resId) ?: return
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                prepare()
                start()
                startFadeIn()
            } else {
                // Custom sound (uri)
                val uri = Uri.parse(sound.id)
                setDataSource(applicationContext, uri)
                setOnPreparedListener {
                    it.start()
                    startFadeIn()
                }
                prepareAsync()
            }
        }

        player = mp

        mp.setOnErrorListener { _, _, _ ->
            val lastSoundId = soundId
            handler.post {
                stopRing()
                startRing(lastSoundId)
                if (reachedFullVolume) {
                    player?.setVolume(1f, 1f)
                }
            }
            true
        }
    }




    private fun stopRing() {
        cancelFade()
        reachedFullVolume = false

        player?.runCatching {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            stop()
            release()
        }
        player = null

        abandonAlarmAudioFocus()
    }


    private fun shouldUseFullScreen(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        return km.isKeyguardLocked
    }


    private fun stopEverything() {
        stopRing()
        stopVibration()
        releaseWakeLock()
        stopForegroundCompat()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun alarmAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private fun requestAlarmAudioFocus(): Boolean {
        val attrs = alarmAudioAttributes()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .setAcceptsDelayedFocusGain(false)
                .build()

            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAlarmAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { req ->
                audioManager.abandonAudioFocusRequest(req)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }


    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
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

    private fun startFadeIn() {
        cancelFade()

        val mp = player ?: return
        val steps = (fadeDurationMs / fadeStepMs).toInt().coerceAtLeast(1)
        var i = 0

        fadeRunnable = object : Runnable {
            override fun run() {
                val p = player
                if (p == null) {
                    cancelFade()
                    return
                }

                i++
                val vol = (i.toFloat() / steps.toFloat()).coerceIn(0f, 1f)
                p.setVolume(vol, vol)

                if (vol >= 1f) {
                    reachedFullVolume = true
                    player?.setVolume(1f, 1f) // фиксируем
                    cancelFade()
                } else {
                    handler.postDelayed(this, fadeStepMs)
                }
            }
        }

        handler.post(fadeRunnable!!)
    }

    private fun cancelFade() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun getVibrator(): Vibrator {
        vibrator?.let { return it }
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        return v
    }

    private fun startVibration() {
        val v = getVibrator()
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Pattern: vibrate / pause / vibrate...
            val timings = longArrayOf(0, 800, 400, 800)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 800, 400, 800), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.runCatching { cancel() }
    }


    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIF_ID = 1001
    }
}
