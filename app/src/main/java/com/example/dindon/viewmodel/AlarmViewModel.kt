package com.example.dindon.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.dindon.model.Alarm
import com.example.dindon.model.AlarmGroup
import com.example.dindon.model.WeekDay

enum class Mode { Custom, Group }

class AlarmViewModel : ViewModel() {

    // Режим экрана
    var mode by mutableStateOf(Mode.Custom)
        private set

    // Будильники (пока заглушки)
    var alarms by mutableStateOf(sampleAlarms())
        private set

    // Группы (считаются из alarms)
    var groups by mutableStateOf(buildGroups(alarms))
        private set

    // Фильтры (null = All)
    var selectedDay by mutableStateOf<WeekDay?>(null)
        private set

    var selectedGroup by mutableStateOf<String?>(null)
        private set

    // Список для отображения (результат фильтров)
    val visibleAlarms: List<Alarm>
        get() = applyFilters(alarms, selectedDay, selectedGroup)

    fun changeMode(newMode: Mode) {
        mode = newMode
    }

    fun toggleAlarm(alarmId: Int, enabled: Boolean) {
        alarms = alarms.map { a -> if (a.id == alarmId) a.copy(enabled = enabled) else a }
        recalcGroupsAndFixFilters()
    }

    fun toggleGroup(groupId: Int, enabled: Boolean) {
        val groupName = groups.firstOrNull { it.id == groupId }?.name ?: return
        alarms = alarms.map { a ->
            if (a.groupName == groupName) a.copy(enabled = enabled) else a
        }
        recalcGroupsAndFixFilters()
    }

    fun setDayFilter(day: WeekDay?) {
        selectedDay = day
    }

    fun setGroupFilter(groupName: String?) {
        selectedGroup = groupName
    }

    fun resetFilters() {
        selectedDay = null
        selectedGroup = null
    }

    fun deleteAlarm(alarmId: Int) {
        alarms = alarms.filterNot { it.id == alarmId }
        recalcGroupsAndFixFilters()
    }

    fun updateAlarm(updated: Alarm) {
        // нормализуем группу (чтобы не было пустых и пробельных)
        val fixed = updated.copy(groupName = normalizeGroupName(updated.groupName))
        alarms = alarms.map { a -> if (a.id == fixed.id) fixed else a }
        recalcGroupsAndFixFilters()
    }

    fun createAlarm(newAlarm: Alarm) {
        val fixed = newAlarm.copy(
            id = nextId(),
            groupName = normalizeGroupName(newAlarm.groupName)
        )
        alarms = alarms + fixed
        recalcGroupsAndFixFilters()
    }

    // ===== Helpers =====

    private fun recalcGroupsAndFixFilters() {
        groups = buildGroups(alarms)

        // если выбранная группа больше не существует — сбросим фильтр группы
        val existing = groups.map { it.name }.toSet()
        if (selectedGroup != null && selectedGroup !in existing) {
            selectedGroup = null
        }
    }

    private fun nextId(): Int = (alarms.maxOfOrNull { it.id } ?: 0) + 1

    private fun normalizeGroupName(raw: String): String {
        val cleaned = raw.trim()
        return if (cleaned.isEmpty()) "Default" else cleaned
    }
}

// ===== Sample data =====

private fun sampleAlarms(): List<Alarm> = listOf(
    Alarm(
        id = 1, hour = 7, minute = 30,
        label = "Подъём",
        groupName = "Work",
        enabled = true,
        days = setOf(WeekDay.Mon, WeekDay.Tue, WeekDay.Wed, WeekDay.Thu, WeekDay.Fri),
        sound = "Default",
        snoozeMinutes = 10,
        vibrate = true
    ),
    Alarm(
        id = 2, hour = 8, minute = 0,
        label = "Запасной",
        groupName = "Work",
        enabled = false,
        days = setOf(WeekDay.Mon, WeekDay.Wed, WeekDay.Fri),
        sound = "Soft",
        snoozeMinutes = 5,
        vibrate = true
    ),
    Alarm(
        id = 3, hour = 18, minute = 15,
        label = "Зал",
        groupName = "Gym",
        enabled = true,
        days = setOf(WeekDay.Tue, WeekDay.Thu, WeekDay.Sat),
        sound = "Loud",
        snoozeMinutes = 10,
        vibrate = false
    ),
    Alarm(
        id = 4, hour = 10, minute = 0,
        label = "Выходной",
        groupName = "Weekend",
        enabled = false,
        days = setOf(WeekDay.Sat, WeekDay.Sun),
        sound = "Default",
        snoozeMinutes = 15,
        vibrate = true
    ),
    Alarm(
        id = 5, hour = 9, minute = 0,
        label = "Каждый день",
        groupName = "Any",
        enabled = true,
        days = emptySet(), // пусто = каждый день
        sound = "Digital",
        snoozeMinutes = 10,
        vibrate = true
    )
)

private fun buildGroups(alarms: List<Alarm>): List<AlarmGroup> {
    val grouped = alarms.groupBy { it.groupName }
    return grouped.entries
        .sortedBy { it.key.lowercase() }
        .mapIndexed { index, (name, list) ->
            val enabledCount = list.count { it.enabled }
            AlarmGroup(
                id = index + 1,
                name = name,
                total = list.size,
                enabledCount = enabledCount
            )
        }
}

private fun applyFilters(
    alarms: List<Alarm>,
    day: WeekDay?,
    groupName: String?
): List<Alarm> {
    return alarms.filter { alarm ->
        val groupOk = groupName == null || alarm.groupName == groupName

        val dayOk = if (day == null) {
            true
        } else {
            // emptySet = каждый день
            alarm.days.isEmpty() || alarm.days.contains(day)
        }

        groupOk && dayOk
    }
}
