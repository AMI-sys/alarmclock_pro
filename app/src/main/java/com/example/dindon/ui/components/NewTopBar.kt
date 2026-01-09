package com.example.dindon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.theme.Neu

@Composable
fun NeuTopBar(
    title: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeuCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        cornerRadius = 18.dp,
        elevation = 6.dp,
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = Neu.onBg.copy(alpha = 0.92f),
                modifier = Modifier.weight(1f)
            )

            // не Material IconButton (он слишком “материалит”),
            // делаем простую кликабельную иконку
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Neu.onBg.copy(alpha = 0.80f)
                )
            }
        }
    }
}
