package com.example.dindon.alarm

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.components.NewButton
import com.example.dindon.ui.theme.DindonTheme
import com.example.dindon.ui.theme.Neu
import com.example.dindon.ui.theme.neuShadow

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(this, null)
        }

        val alarmId = intent.getIntExtra(AlarmActions.EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(AlarmActions.EXTRA_LABEL) ?: "Будильник"
        val soundId = intent.getStringExtra(AlarmActions.EXTRA_SOUND_ID) ?: "american"
        val vibrate = intent.getBooleanExtra(AlarmActions.EXTRA_VIBRATE, true)
        val snoozeMin = intent.getIntExtra(AlarmActions.EXTRA_SNOOZE_MIN, 5)

        setContent {
            DindonTheme {
                AlarmRingScreen(
                    label = label,
                    onDismiss = {
                        startForegroundService(AlarmForegroundServiceIntent.dismiss(this@AlarmRingActivity, alarmId))
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
        fun dismiss(context: android.content.Context, alarmId: Int) =
            android.content.Intent(context, AlarmForegroundService::class.java).apply {
                action = AlarmActions.ACTION_DISMISS
                putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Neu.bg)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .neuShadow(cornerRadius = 999.dp, elevation = 8.dp)
                    .clip(CircleShape)
                    .background(Neu.bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u23F0",
                    style = MaterialTheme.typography.h3
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = label.ifBlank { "Будильник" },
                style = MaterialTheme.typography.h5,
                color = Neu.onBg.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NewButton(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 8.dp,
                onClick = onSnooze
            ) {
                Text(
                    text = "Отложить",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.90f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            NewButton(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 8.dp,
                onClick = onDismiss
            ) {
                Text(
                    text = "Выключить",
                    style = MaterialTheme.typography.h6,
                    color = Neu.onBg.copy(alpha = 0.80f)
                )
            }
        }
    }
}