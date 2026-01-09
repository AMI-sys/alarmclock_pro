package com.example.dindon.ui.screens

import android.widget.NumberPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import com.example.dindon.ui.sound.AlarmSounds
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

    var soundId by remember { mutableStateOf(initial.sound) }
    var snoozeMinutes by remember { mutableStateOf(initial.snoozeMinutes) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }

    var groupMenuExpanded by remember { mutableStateOf(false) }
    var soundMenuExpanded by remember { mutableStateOf(false) }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    val selectedSound = remember(soundId) {
        AlarmSounds.byId(soundId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set alarm") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(
                            initial.copy(
                                hour = hour,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                groupName = group.ifBlank { "Default" },
                                days = days,
                                sound = soundId,
                                snoozeMinutes = snoozeMinutes,
                                vibrate = vibrate
                            )
                        )
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(vScroll)
                .padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumberPickerField(hour, 0..23) { hour = it }
                NumberPickerField(minute, 0..59) { minute = it }
            }

            Spacer(Modifier.height(16.dp))

            Card {
                Column(Modifier.padding(16.dp)) {

                    Text("Repeat")
                    Spacer(Modifier.height(8.dp))

                    Row(
                        Modifier.horizontalScroll(hScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeekDay.values().forEach { day ->
                            DayChip(
                                text = day.short,
                                selected = days.contains(day)
                            ) {
                                days =
                                    if (days.contains(day)) days - day
                                    else days + day
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))

                    SettingRowClickable("Group", group) {
                        groupMenuExpanded = true
                    }

                    DropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false }
                    ) {
                        groupSuggestions.distinct().forEach { g ->
                            DropdownMenuItem(onClick = {
                                group = g
                                groupMenuExpanded = false
                            }) {
                                Text(g)
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(Modifier.padding(vertical = 12.dp))

                    SettingRowClickable("Alarm sound", selectedSound.title) {
                        soundMenuExpanded = true
                    }

                    DropdownMenu(
                        expanded = soundMenuExpanded,
                        onDismissRequest = { soundMenuExpanded = false }
                    ) {
                        AlarmSounds.all.forEach { s ->
                            DropdownMenuItem(onClick = {
                                soundId = s.id
                                soundMenuExpanded = false
                            }) {
                                Text(s.title)
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))

                    Text("Snooze: $snoozeMinutes min")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = {
                            snoozeMinutes = max(1, snoozeMinutes - 1)
                        }) { Text("−") }

                        OutlinedButton(onClick = {
                            snoozeMinutes = min(60, snoozeMinutes + 1)
                        }) { Text("+") }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibration")
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

/* ---------- helpers ---------- */

@Composable
private fun SettingRowClickable(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value)
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}

@Composable
private fun DayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        backgroundColor =
            if (selected) MaterialTheme.colors.primary.copy(alpha = 0.25f)
            else MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
        elevation = 0.dp
    ) {
        TextButton(onClick = onClick) {
            Text(text)
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
                setFormatter { it.toString().padStart(2, '0') }
                setOnValueChangedListener { _, _, new ->
                    onValueChange(new)
                }
                this.value = value
            }
        },
        update = {
            if (it.value != value) it.value = value
        }
    )
}
