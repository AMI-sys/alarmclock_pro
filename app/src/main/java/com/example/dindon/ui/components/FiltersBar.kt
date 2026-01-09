package com.example.dindon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.model.WeekDay
import com.example.dindon.ui.theme.Neu

@Composable
fun FiltersBar(
    selectedDay: WeekDay?,
    selectedGroup: String?,
    groups: List<String>,
    onDayChanged: (WeekDay?) -> Unit,
    onGroupChanged: (String?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var groupMenu by remember { mutableStateOf(false) }

    NeuCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        cornerRadius = 18.dp,
        elevation = 6.dp,
        contentPadding = 12.dp
    ) {
        Column {
            // --- Days chips ---
            Text(
                text = "Day",
                style = MaterialTheme.typography.caption,
                color = Neu.onBg.copy(alpha = 0.60f)
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item {
                    NeuChip(
                        text = "All",
                        selected = selectedDay == null,
                        onClick = { onDayChanged(null) }
                    )
                    Spacer(Modifier.width(8.dp))
                }

                items(WeekDay.values()) { day ->
                    NeuChip(
                        text = day.short,
                        selected = selectedDay == day,
                        onClick = { onDayChanged(day) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Group dropdown + reset ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Group selector (не Material ExposedDropdown — чтобы не тянуть M3)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Group",
                        style = MaterialTheme.typography.caption,
                        color = Neu.onBg.copy(alpha = 0.60f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Box {
                        // "кнопка" открытия меню
                        NeuChip(
                            text = selectedGroup ?: "All groups",
                            selected = selectedGroup != null,
                            onClick = { groupMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = groupMenu,
                            onDismissRequest = { groupMenu = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                groupMenu = false
                                onGroupChanged(null)
                            }) { Text("All groups") }

                            groups.distinct().forEach { g ->
                                DropdownMenuItem(onClick = {
                                    groupMenu = false
                                    onGroupChanged(g)
                                }) { Text(g) }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Reset
                NewButton(
                    onClick = onReset,
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.button,
                        color = Neu.onBg.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
