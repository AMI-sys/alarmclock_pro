package com.example.dindon.ui.screens

import android.widget.NumberPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import kotlin.math.max
import kotlin.math.min

@Composable
fun AlarmEditScreen(
    initial: Alarm,
    groupSuggestions: List<String>,
    onCancel: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var label by remember { mutableStateOf(initial.label) }
    var group by remember { mutableStateOf(initial.groupName) }
    var days by remember { mutableStateOf(initial.days) }

    // NEW settings
    var sound by remember { mutableStateOf(initial.sound) }
    var snoozeMinutes by remember { mutableStateOf(initial.snoozeMinutes) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }

    // group dropdown
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // sound dropdown
    val soundOptions = listOf("Default", "Soft", "Loud", "Beep", "Digital")
    var soundMenuExpanded by remember { mutableStateOf(false) }

    if (showCreateGroup) {
        AlertDialog(
            onDismissRequest = { showCreateGroup = false },
            title = { Text("New group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleaned = newGroupName.trim()
                    if (cleaned.isNotEmpty()) group = cleaned
                    newGroupName = ""
                    showCreateGroup = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newGroupName = ""
                    showCreateGroup = false
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set alarm") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Text("✕") }
                },
                actions = {
                    IconButton(onClick = {
                        val cleanedGroup = group.trim().ifEmpty { "Default" }
                        val cleanedLabel = label.trim().ifEmpty { "Alarm" }

                        onSave(
                            initial.copy(
                                hour = hour,
                                minute = minute,
                                label = cleanedLabel,
                                groupName = cleanedGroup,
                                days = days,
                                sound = sound,
                                snoozeMinutes = snoozeMinutes,
                                vibrate = vibrate
                            )
                        )
                    }) { Text("✓") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // ===== TIME (NumberPicker like screenshot) =====
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumberPickerField(value = hour, range = 0..23, onValueChange = { hour = it })
                NumberPickerField(value = minute, range = 0..59, onValueChange = { minute = it })
            }

            Spacer(Modifier.height(16.dp))

            // ===== GLASS-LIKE SETTINGS CARD (fast, no blur) =====
            Card(
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {

                    // Repeat (days)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Repeat")
                        Text(if (days.isEmpty()) "Everyday" else "${days.size} day(s)", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WeekDay.values().toList()) { day ->
                            val selected = days.contains(day)
                            val bg = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.06f)

                            Card(backgroundColor = bg, elevation = 0.dp) {
                                TextButton(
                                    onClick = { days = if (selected) days - day else days + day },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text(day.short) }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))

                    // Group
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Group")
                        Text(group.ifBlank { "Default" }, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { groupMenuExpanded = true }) { Text("Choose") }
                    }

                    DropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            groupMenuExpanded = false
                            showCreateGroup = true
                        }) { Text("➕ Create new…") }

                        groupSuggestions
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .sortedBy { it.lowercase() }
                            .forEach { g ->
                                DropdownMenuItem(onClick = {
                                    group = g
                                    groupMenuExpanded = false
                                }) { Text(g) }
                            }
                    }

                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))

                    // Label
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))

                    // Sound
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Alarm sound")
                        Text(sound, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { soundMenuExpanded = true }) { Text("Choose") }
                    }
                    DropdownMenu(
                        expanded = soundMenuExpanded,
                        onDismissRequest = { soundMenuExpanded = false }
                    ) {
                        soundOptions.forEach { s ->
                            DropdownMenuItem(onClick = {
                                sound = s
                                soundMenuExpanded = false
                            }) { Text(s) }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))

                    // Snooze
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Snooze")
                        Text("$snoozeMinutes min", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { snoozeMinutes = max(1, snoozeMinutes - 1) }) { Text("−") }
                        TextButton(onClick = { snoozeMinutes = min(60, snoozeMinutes + 1) }) { Text("+") }
                    }

                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))

                    // Vibration
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vibration")
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // подсказка UX
            Text(
                text = "Tip: tap days to set repeat. Empty days = Everyday.",
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
private fun NumberPickerField(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true
                setFormatter { v -> v.toString().padStart(2, '0') }
                setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                this.value = value
            }
        },
        update = { picker ->
            picker.minValue = range.first
            picker.maxValue = range.last
            if (picker.value != value) picker.value = value
        }
    )
}
