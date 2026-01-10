package com.example.dindon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import com.example.dindon.ui.components.*
import com.example.dindon.ui.theme.Neu


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vibrateByDefault: Boolean,
    onVibrateByDefaultChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        NeuTopBar(
            title = "Settings",
            onNavigation = onBack,
            showSettings = false
        )

        Spacer(modifier = Modifier.height(20.dp))

        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevation = 6.dp,
            contentPadding = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Defaults", count = null)
                Spacer(modifier = Modifier.height(16.dp))

                RowSetting(
                    title = "Vibration by default",
                    subtitle = "New alarms will have vibration enabled",
                    toggleState = vibrateByDefault,
                    onToggleChange = onVibrateByDefaultChanged
                )
            }
        }
    }
}

@Composable
private fun RowSetting(
    title: String,
    subtitle: String,
    toggleState: Boolean,
    onToggleChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Neu.onBg.copy(alpha = 0.92f),
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Neu.onBg.copy(alpha = 0.65f),
                style = MaterialTheme.typography.body2
            )
        }

        NewToggle(checked = toggleState, onCheckedChange = onToggleChange)
    }
}
