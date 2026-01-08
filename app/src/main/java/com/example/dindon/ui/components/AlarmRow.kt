package com.example.dindon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.model.Alarm
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Кликабельная только левая часть (текст)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit(alarm.id) }
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = formatTime(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.h6
                )
                Spacer(Modifier.height(2.dp))
                Text(text = alarm.label, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Group: ${alarm.groupName}",
                    style = MaterialTheme.typography.caption
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = { onToggle(alarm.id, it) }
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("⋮")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(onClick = {
                        menuExpanded = false
                        onEdit(alarm.id)
                    }) {
                        Text("Edit")
                    }
                    DropdownMenuItem(onClick = {
                        menuExpanded = false
                        confirmDelete = true
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

private fun formatTime(h: Int, m: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", h, m)
