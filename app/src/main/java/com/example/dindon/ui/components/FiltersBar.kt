package com.example.dindon.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.model.WeekDay

@Composable
fun FiltersBar(
    selectedDay: WeekDay?,
    selectedGroup: String?,
    groups: List<String>,
    onDayChanged: (WeekDay?) -> Unit,
    onGroupChanged: (String?) -> Unit,
    onReset: () -> Unit
) {
    val hasActiveFilters = selectedDay != null || selectedGroup != null

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- Day filter row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Лейбл (делает яснее, что это фильтр)
            Text("Day:", modifier = Modifier.padding(end = 8.dp))

            DayButton(
                text = "All",
                selected = selectedDay == null,
                onClick = { onDayChanged(null) }
            )

            Spacer(Modifier.width(8.dp))

            WeekDay.values().forEach { day ->
                DayButton(
                    text = day.short,
                    selected = selectedDay == day,
                    onClick = { onDayChanged(day) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Group filter row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupFilterButton(
                selectedGroup = selectedGroup,
                groups = groups,
                onGroupChanged = onGroupChanged
            )

            Spacer(Modifier.weight(1f))

            if (hasActiveFilters) {
                TextButton(onClick = onReset) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun DayButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (selected) "$text ✓" else text)
    }
}

@Composable
private fun GroupFilterButton(
    selectedGroup: String?,
    groups: List<String>,
    onGroupChanged: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val value = selectedGroup ?: "All"
    val label = "Group: $value ▾"

    TextButton(onClick = { expanded = true }) {
        Text(label)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(onClick = {
            expanded = false
            onGroupChanged(null)
        }) { Text("All") }

        groups.forEach { g ->
            DropdownMenuItem(onClick = {
                expanded = false
                onGroupChanged(g)
            }) { Text(g) }
        }
    }
}
