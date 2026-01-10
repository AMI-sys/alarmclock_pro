package com.example.dindon

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dindon.ui.screens.MainScreen
import com.example.dindon.ui.theme.DindonTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Не важно, что выбрал пользователь — продолжаем цепочку
        runPermissionChecks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DindonTheme {
                MainScreen()
            }
        }

        runPermissionChecks()
    }

    override fun onResume() {
        super.onResume()
        runPermissionChecks()
    }

    private fun runPermissionChecks() {
        if (!ensureNotificationPermission()) return
        if (!ensureExactAlarmsPermission()) return
        if (!ensureFullScreenIntentAccess()) return

        // Опционально (если хочешь “железобетон” на отдельных прошивках)
        // ensureOverlayPermission()
    }

    private fun ensureNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val granted =
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) return true

        AlertDialog.Builder(this)
            .setTitle("Разрешение на уведомления")
            .setMessage("Нужно, чтобы будильник мог показывать уведомление и полноэкранный экран при срабатывании.")
            .setPositiveButton("Разрешить") { _: DialogInterface, _: Int ->
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Не сейчас") { _: DialogInterface, _: Int ->
                Toast.makeText(
                    this,
                    "Без уведомлений полноэкранный показ может не работать.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()

        return false
    }

    private fun ensureExactAlarmsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) return true

        AlertDialog.Builder(this)
            .setTitle("Точные будильники")
            .setMessage("Нужно разрешить «Будильники и напоминания», иначе будильник может не сработать вовремя.")
            .setPositiveButton("Открыть настройки") { _: DialogInterface, _: Int ->
                runCatching {
                    val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(i)
                }
            }
            .setNegativeButton("Не сейчас") { _: DialogInterface, _: Int -> }
            .show()

        return false
    }

    private fun ensureFullScreenIntentAccess(): Boolean {
        // Android 14+: у пользователя может быть выключен спец-доступ full screen intents
        if (Build.VERSION.SDK_INT < 34) return true

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.canUseFullScreenIntent()) return true

        AlertDialog.Builder(this)
            .setTitle("Полноэкранный показ будильника")
            .setMessage("Разреши «Full screen intents», чтобы будильник сам открывал экран поверх блокировки.")
            .setPositiveButton("Открыть настройки") { _: DialogInterface, _: Int ->
                runCatching {
                    val i = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(i)
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Не удалось открыть настройки Full screen intents",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Не сейчас") { _: DialogInterface, _: Int -> }
            .show()

        return false
    }

    // Если решишь включить overlay — раскомментируй вызов в runPermissionChecks()
    @Suppress("unused")
    private fun ensureOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (Settings.canDrawOverlays(this)) return true

        AlertDialog.Builder(this)
            .setTitle("Поверх других приложений")
            .setMessage("На некоторых устройствах это помогает гарантированно показать экран будильника поверх блокировки.")
            .setPositiveButton("Открыть настройки") { _: DialogInterface, _: Int ->
                try {
                    val i = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(i)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, "Не удалось открыть настройки overlay", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Не сейчас") { _: DialogInterface, _: Int -> }
            .show()

        return false
    }
}
