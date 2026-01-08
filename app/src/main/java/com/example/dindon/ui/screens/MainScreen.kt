package com.example.dindon.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import com.example.dindon.ui.components.AlarmRow
import com.example.dindon.ui.components.FiltersBar
import com.example.dindon.ui.components.GroupRow
import com.example.dindon.ui.components.ModeToggle
import com.example.dindon.viewmodel.AlarmViewModel
import com.example.dindon.viewmodel.Mode

private const val NEW_ALARM_ID = -1

@Composable
fun MainScreen(vm: AlarmViewModel = viewModel()) {

    // editId == null => list screen
    // editId == -1   => create new
    // else           => edit existing
    var editingId by remember { mutableStateOf<Int?>(null) }

    val groupNames = vm.groups.map { it.name }

    // ===== FULL SCREEN EDITOR =====
    if (editingId != null) {
        val initial = if (editingId == NEW_ALARM_ID) {
            defaultNewAlarm(groupNames)
        } else {
            vm.alarms.firstOrNull { it.id == editingId } ?: run {
                // если вдруг удалили будильник пока открыт
                editingId = null
                return
            }
        }

        AlarmEditScreen(
            initial = initial,
            groupSuggestions = groupNames,
            onCancel = { editingId = null },
            onSave = { updated ->
                if (editingId == NEW_ALARM_ID) vm.createAlarm(updated) else vm.updateAlarm(updated)
                editingId = null
            }
        )
        return
    }

    // ===== LIST SCREEN =====
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dindon Alarm") },
                actions = {
                    ModeToggle(
                        mode = vm.mode,
                        onModeChanged = vm::changeMode
                    )
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (vm.mode == Mode.Custom) {
                FloatingActionButton(onClick = { editingId = NEW_ALARM_ID }) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm")
                }
            }
        }
    ) { padding ->

        when (vm.mode) {
            Mode.Custom -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    FiltersBar(
                        selectedDay = vm.selectedDay,
                        selectedGroup = vm.selectedGroup,
                        groups = groupNames,
                        onDayChanged = vm::setDayFilter,
                        onGroupChanged = vm::setGroupFilter,
                        onReset = vm::resetFilters
                    )

                    val sorted = vm.visibleAlarms.sortedWith(compareBy<Alarm>({ it.hour }, { it.minute }))

                    val selectedDay = vm.selectedDay
                    val everyday = sorted.filter { it.days.isEmpty() }

                    val dayList = if (selectedDay != null) {
                        sorted.filter { it.days.contains(selectedDay) }
                    } else emptyList()

                    val nothingToShow =
                        if (selectedDay == null) sorted.isEmpty()
                        else (everyday.isEmpty() && dayList.isEmpty())

                    if (nothingToShow) {
                        EmptyState(onClear = vm::resetFilters)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {

                            if (everyday.isNotEmpty()) {
                                item { SectionHeader(title = "Everyday", count = everyday.size) }
                                items(everyday, key = { "every_${it.id}" }) { alarm ->
                                    AlarmRow(
                                        alarm = alarm,
                                        onToggle = vm::toggleAlarm,
                                        onEdit = { id -> editingId = id }, // ✅ тап по карточке
                                        onDelete = vm::deleteAlarm
                                    )
                                }
                            }

                            if (selectedDay != null) {
                                if (dayList.isNotEmpty()) {
                                    item { SectionHeader(title = selectedDay.short, count = dayList.size) }
                                    items(dayList, key = { "${selectedDay.name}_${it.id}" }) { alarm ->
                                        AlarmRow(
                                            alarm = alarm,
                                            onToggle = vm::toggleAlarm,
                                            onEdit = { id -> editingId = id },
                                            onDelete = vm::deleteAlarm
                                        )
                                    }
                                }
                            } else {
                                WeekDay.values().forEach { day ->
                                    val list = sorted.filter { it.days.contains(day) }
                                    if (list.isNotEmpty()) {
                                        item { SectionHeader(title = day.short, count = list.size) }
                                        items(list, key = { "${day.name}_${it.id}" }) { alarm ->
                                            AlarmRow(
                                                alarm = alarm,
                                                onToggle = vm::toggleAlarm,
                                                onEdit = { id -> editingId = id },
                                                onDelete = vm::deleteAlarm
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(96.dp)) } // место под FAB
                        }
                    }
                }
            }

            Mode.Group -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(vm.groups, key = { it.id }) { group ->
                        GroupRow(group = group, onToggleGroup = vm::toggleGroup)
                    }
                }
            }
        }
    }
}

private fun defaultNewAlarm(groupSuggestions: List<String>): Alarm {
    val group = groupSuggestions.firstOrNull() ?: "Default"

    return Alarm(
        id = NEW_ALARM_ID, // будет заменён в createAlarm()
        hour = 7,
        minute = 0,
        label = "",
        groupName = group,
        enabled = true,
        days = emptySet(),

        // новые настройки
        sound = "Default",
        snoozeMinutes = 10,
        vibrate = true
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$title  •  $count",
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

//metch
@Composable
private fun EmptyState(onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("No alarms found", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text("Try changing filters or create a new alarm.", style = MaterialTheme.typography.body2)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClear) { Text("Clear filters") }
    }
}
