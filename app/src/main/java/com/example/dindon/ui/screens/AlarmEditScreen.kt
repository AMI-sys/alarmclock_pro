package com.example.dindon.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import com.example.dindon.ui.components.*
import com.example.dindon.ui.sound.AlarmSounds
import com.example.dindon.ui.vibration.VibrationPatterns
import kotlin.math.max
import kotlin.math.min

@Composable
fun AlarmEditScreen(
    initial: Alarm,
    groupSuggestions: List<String>,
    onCancel: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var hour by rememberSaveable { mutableStateOf(initial.hour) }
    var minute by rememberSaveable { mutableStateOf(initial.minute) }
    var label by rememberSaveable { mutableStateOf(initial.label) }
    var group by rememberSaveable { mutableStateOf(initial.groupName) }
    var days by rememberSaveable { mutableStateOf(initial.days.toSet()) }
    var soundId by rememberSaveable { mutableStateOf(initial.sound) }
    var snoozeMinutes by rememberSaveable { mutableStateOf(initial.snoozeMinutes) }
    var vibrate by rememberSaveable { mutableStateOf(initial.vibrate) }
    var vibrationPattern by rememberSaveable {
        mutableStateOf(initial.vibrationPattern ?: "pulse")
    }
    var showVibrationPicker by remember { mutableStateOf(false) }

    val selectedSound = remember(soundId) {
        when (soundId) {
            AlarmSounds.NONE_ID -> AlarmSounds.Sound(AlarmSounds.NONE_ID, "Без звука", null)
            else -> AlarmSounds.all.find { it.id == soundId }
                ?: AlarmSounds.Sound(soundId, "Пользовательский файл", null)
        }
    }


    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var wasEdited by remember { mutableStateOf(false) }

    var showSoundPicker by remember { mutableStateOf(false) }
    val customSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) cursor.getString(nameIndex) else "Пользовательский файл"
            } ?: "Пользовательский файл"

            soundId = it.toString()
            AlarmSounds.registerCustomSound(soundId, name) // см. ниже
            wasEdited = true
        }
    }

    val isSoundOff = soundId == AlarmSounds.NONE_ID
    val mode = when {
        isSoundOff && vibrate -> "vibrate_only"
        !isSoundOff && vibrate -> "sound_and_vibrate"
        else -> "sound_only"
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Несохраненные изменения") },
            text = { Text("У вас есть несохраненные изменения. Выйти без сохранения?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onCancel()
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            NeuTopBar(
                title = "Будильник",
                showSettings = false,
                onNavigation = {
                    if (wasEdited) showUnsavedDialog = true else onCancel()
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                onDelete?.let {
                    NewButton(onClick = onDelete) { Text("Удалить") }
                }
                NewButton(onClick = {
                    onSave(
                        initial.copy(
                            hour = hour,
                            minute = minute,
                            label = label.ifBlank { "Alarm" },
                            groupName = group.ifBlank { "Default" },
                            days = days,
                            sound = soundId,
                            snoozeMinutes = snoozeMinutes,
                            vibrate = vibrate,
                            vibrationPattern = vibrationPattern
                        )
                    )
                    Toast.makeText(context, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Сохранить")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumberPickerField(hour, 0..23) {
                        hour = it
                        wasEdited = true
                    }
                    NumberPickerField(minute, 0..59) {
                        minute = it
                        wasEdited = true
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Повтор", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeekDay.values().forEach { day ->
                            NeuChip(
                                text = day.short,
                                selected = days.contains(day),
                                onClick = {
                                    days = if (days.contains(day)) days - day else days + day
                                    wasEdited = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                SettingRowInput("Метка", label) {
                    label = it
                    wasEdited = true
                }
            }

            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                SettingRowInput("Группа", group) {
                    group = it
                    wasEdited = true
                }
            }

            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                SettingRowClickable("Мелодия", selectedSound.title) {
                    showSoundPicker = true
                }
            }

            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Повтор сигнала: $snoozeMinutes мин", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NewButton(onClick = {
                            snoozeMinutes = max(1, snoozeMinutes - 1)
                            wasEdited = true
                        }) { Text("−") }

                        NewButton(onClick = {
                            snoozeMinutes = min(60, snoozeMinutes + 1)
                            wasEdited = true
                        }) { Text("+") }
                    }
                }
            }


            Spacer(Modifier.height(16.dp))

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Вибрация", style = MaterialTheme.typography.body1)
                    NewToggle(checked = vibrate, onCheckedChange = {
                        vibrate = it
                        wasEdited = true
                    })
                }
            }

            if (vibrate) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRowClickable(
                        title = "Vibration type",
                        value = VibrationPatterns.titleFor(vibrationPattern),
                        onClick = { showVibrationPicker = true }
                    )
                }
            }

            if (showSoundPicker) {
                AlertDialog(
                    onDismissRequest = { showSoundPicker = false },
                    title = { Text("Выберите мелодию") },
                    text = {
                        Column {
                            LazyColumn {
                                item {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                soundId = AlarmSounds.NONE_ID
                                                showSoundPicker = false
                                                wasEdited = true
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Без звука", Modifier.weight(1f))
                                        if (soundId == AlarmSounds.NONE_ID) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                }

                                items(AlarmSounds.all) { sound ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                soundId = sound.id
                                                showSoundPicker = false
                                                wasEdited = true
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sound.title, Modifier.weight(1f))
                                        if (sound.id == soundId) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                }

                                item {
                                    if (soundId != AlarmSounds.NONE_ID && AlarmSounds.all.none { it.id == soundId }) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showSoundPicker = false
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Пользовательский файл", Modifier.weight(1f))
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                }


                            }
                            Spacer(Modifier.height(8.dp))
                            NewButton(onClick = {
                                showSoundPicker = false
                                customSoundLauncher.launch(arrayOf("audio/*"))
                            }) {
                                Text("Выбрать свой файл")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }

            if (showVibrationPicker) {
                AlertDialog(
                    onDismissRequest = { showVibrationPicker = false },
                    title = { Text("Vibration type") },
                    text = {
                        Column {
                            VibrationPatterns.all.forEach { pattern ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vibrationPattern = pattern.id
                                            showVibrationPicker = false
                                            wasEdited = true
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pattern.title, Modifier.weight(1f))
                                    if (pattern.id == vibrationPattern) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }

            Spacer(Modifier.height(80.dp))
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
                setOnValueChangedListener { _, _, new -> onValueChange(new) }
                this.value = value
            }
        },
        update = { if (it.value != value) it.value = value }
    )
}

@Composable
private fun SettingRowClickable(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(max = 180.dp) // можно подправить
        ) {
            Text(
                text = value,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

    }
}

@Composable
private fun SettingRowInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    )
}
