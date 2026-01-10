package com.example.dindon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import com.example.dindon.ui.components.*
import com.example.dindon.ui.theme.Neu
import com.example.dindon.viewmodel.AlarmViewModel
import com.example.dindon.viewmodel.Mode

private const val NEW_ALARM_ID = -1

@Composable
fun MainScreen(vm: AlarmViewModel = viewModel()) {

    var editingId by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var defaultVibrate by remember { mutableStateOf(true) }

    val groupNames = vm.groups.map { it.name }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            vibrateByDefault = defaultVibrate,
            onVibrateByDefaultChanged = { defaultVibrate = it }
        )
        return
    }

    if (editingId != null) {
        val initial = if (editingId == NEW_ALARM_ID) {
            defaultNewAlarm(groupNames, defaultVibrate)
        } else {
            vm.alarms.firstOrNull { it.id == editingId } ?: run {
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

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (vm.mode == Mode.Custom) {
                NeuFab(onClick = { editingId = NEW_ALARM_ID }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add alarm",
                        tint = Neu.onBg.copy(alpha = 0.9f)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NeuTopBar(
                title = "Dindon Alarm",
                onNavigation = { showSettings = true },
                showSettings = true
            )

            NeuSegmentedControl(
                leftText = "Alarms",
                rightText = "Groups",
                selectedIndex = if (vm.mode == Mode.Group) 1 else 0,
                onSelect = { index ->
                    vm.changeMode(if (index == 1) Mode.Group else Mode.Custom)
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )

            when (vm.mode) {
                Mode.Custom -> {
                    FiltersBar(
                        selectedDay = vm.selectedDay,
                        selectedGroup = vm.selectedGroup,
                        groups = groupNames,
                        onDayChanged = vm::setDayFilter,
                        onGroupChanged = vm::setGroupFilter,
                        onReset = vm::resetFilters
                    )

                    val sorted = vm.visibleAlarms
                        .sortedWith(compareBy<Alarm>({ it.hour }, { it.minute }))
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
                                        onEdit = { id -> editingId = id },
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

                            item { Spacer(Modifier.height(96.dp)) }
                        }
                    }
                }

                Mode.Group -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(vm.groups, key = { it.id }) { group ->
                            GroupRow(group = group, onToggleGroup = vm::toggleGroup)
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

private fun defaultNewAlarm(groupSuggestions: List<String>, defaultVibrate: Boolean): Alarm {
    val group = groupSuggestions.firstOrNull() ?: "Default"
    return Alarm(
        id = NEW_ALARM_ID,
        hour = 7,
        minute = 0,
        label = "",
        groupName = group,
        enabled = true,
        days = emptySet(),
        sound = "Default",
        snoozeMinutes = 10,
        vibrate = defaultVibrate
    )
}
