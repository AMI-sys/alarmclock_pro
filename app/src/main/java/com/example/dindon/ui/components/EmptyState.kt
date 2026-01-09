package com.example.dindon.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.theme.Neu

@Composable
fun EmptyState(
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeuCard(
        modifier = modifier.padding(16.dp),
        cornerRadius = 18.dp,
        elevation = 6.dp,
        contentPadding = 18.dp
    ) {
        Column {
            Text(
                text = "No alarms found",
                style = MaterialTheme.typography.h6,
                color = Neu.onBg.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Try changing filters or create a new alarm.",
                style = MaterialTheme.typography.body2,
                color = Neu.onBg.copy(alpha = 0.70f)
            )
            Spacer(Modifier.height(14.dp))

            // Не используем Material TextButton — делаем вашу кнопку-объект
            NewButton(onClick = onClear) {
                Text(
                    text = "Clear filters",
                    style = MaterialTheme.typography.button,
                    color = Neu.onBg.copy(alpha = 0.9f)
                )
            }
        }
    }
}
