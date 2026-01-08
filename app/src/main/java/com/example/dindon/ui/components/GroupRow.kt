package com.example.dindon.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.model.AlarmGroup
import com.example.dindon.model.GroupState

@Composable
fun GroupRow(
    group: AlarmGroup,
    onToggleGroup: (Int, Boolean) -> Unit
) {
    val stateText = when (group.state) {
        GroupState.Empty -> "Empty"
        GroupState.AllOff -> "All OFF"
        GroupState.AllOn -> "All ON"
        GroupState.Mixed -> "Mixed"
    }

    val checked = group.state == GroupState.AllOn
    val enabled = group.state != GroupState.Empty

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
            Text("${group.total} alarms • $stateText", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onToggleGroup(group.id, it) }
        )
    }
    Divider()
}
