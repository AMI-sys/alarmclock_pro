package com.example.dindon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.model.AlarmGroup
import com.example.dindon.model.GroupState
import com.example.dindon.ui.theme.Neu

@Composable
fun GroupRow(
    group: AlarmGroup,
    onToggleGroup: (Int, Boolean) -> Unit
) {
    val enabled = group.state != GroupState.Empty
    val checked = group.state == GroupState.AllOn

    val stateText = when (group.state) {
        GroupState.Empty -> "Empty"
        GroupState.AllOff -> "All off"
        GroupState.AllOn -> "All on"
        GroupState.Mixed -> "Mixed"
    }

    val contentAlpha = if (enabled) 1f else 0.4f

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(
                if (enabled)
                    Modifier.clickable { onToggleGroup(group.id, !checked) }
                else Modifier
            ),
        cornerRadius = 18.dp,
        elevation = 9.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT — text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.subtitle1,
                    color = Neu.onBg.copy(alpha = 0.9f * contentAlpha)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${group.total} alarms • $stateText",
                    style = MaterialTheme.typography.caption,
                    color = Neu.onBg.copy(alpha = 0.65f * contentAlpha)
                )
            }

            // RIGHT — toggle
            NewToggle(
                checked = checked,
                onCheckedChange = {
                    if (enabled) {
                        onToggleGroup(group.id, it)
                    }
                }
            )
        }
    }
}
