package com.example.dindon.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.theme.DindonTheme

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Показ поверх lockscreen + включить экран
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val alarmId = intent.getIntExtra(AlarmActions.EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(AlarmActions.EXTRA_LABEL) ?: "Alarm"
        val soundId = intent.getStringExtra(AlarmActions.EXTRA_SOUND_ID) ?: "clock"
        val vibrate = intent.getBooleanExtra(AlarmActions.EXTRA_VIBRATE, true)
        val snoozeMin = intent.getIntExtra(AlarmActions.EXTRA_SNOOZE_MIN, 5)

        setContent {
            DindonTheme {
                AlarmRingScreen(
                    label = label,
                    onDismiss = {
                        startForegroundService(
                            AlarmForegroundServiceIntent.dismiss(this@AlarmRingActivity)
                        )
                        finish()
                    },
                    onSnooze = {
                        startForegroundService(
                            AlarmForegroundServiceIntent.snooze(
                                this@AlarmRingActivity,
                                alarmId, label, soundId, vibrate, snoozeMin
                            )
                        )
                        finish()
                    }
                )
            }
        }
    }

    private object AlarmForegroundServiceIntent {
        fun dismiss(context: android.content.Context) =
            android.content.Intent(context, AlarmForegroundService::class.java).apply {
                action = AlarmActions.ACTION_DISMISS
            }

        fun snooze(
            context: android.content.Context,
            alarmId: Int,
            label: String,
            soundId: String,
            vibrate: Boolean,
            snoozeMin: Int
        ) =
            android.content.Intent(context, AlarmForegroundService::class.java).apply {
                action = AlarmActions.ACTION_SNOOZE
                putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmActions.EXTRA_LABEL, label)
                putExtra(AlarmActions.EXTRA_SOUND_ID, soundId)
                putExtra(AlarmActions.EXTRA_VIBRATE, vibrate)
                putExtra(AlarmActions.EXTRA_SNOOZE_MIN, snoozeMin)
            }
    }
}

@Composable
private fun AlarmRingScreen(
    label: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏰",
                style = MaterialTheme.typography.h2
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.h5
            )

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSnooze) { Text("Snooze") }
                Button(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
