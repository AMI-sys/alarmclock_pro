package com.example.dindon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dindon.model.Alarm
import com.example.dindon.ui.theme.Neu
import java.util.Locale

@Composable
fun AlarmRow(
    alarm: Alarm,
    onToggle: (Int, Boolean) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete alarm?") },
            text = { Text("${formatTime(alarm.hour, alarm.minute)} • ${alarm.label}") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(alarm.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    val contentAlpha = if (alarm.enabled) 1f else 0.45f

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onEdit(alarm.id) },
        cornerRadius = 18.dp,
        elevation = 10.dp,
        contentPadding = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT — content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = formatTime(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.h3,
                    color = LocalContentColor.current.copy(alpha = contentAlpha)
                )

                if (alarm.label.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.body2,
                        color = LocalContentColor.current.copy(alpha = 0.7f * contentAlpha)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallChip(text = alarm.groupName)
                }
            }

            // RIGHT — controls
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                NewToggle(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle(alarm.id, it) }
                )

                Spacer(Modifier.height(8.dp))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            "⋯",
                            style = MaterialTheme.typography.h6,
                            color = Neu.onBg.copy(alpha = 0.6f)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            onEdit(alarm.id)
                        }) { Text("Edit") }

                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            confirmDelete = true
                        }) {
                            Text("Delete", color = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallChip(text: String) {
    Surface(
        color = Neu.outline,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.Black.copy(alpha = 0.65f)
        )
    }
}

private fun formatTime(h: Int, m: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", h, m)
