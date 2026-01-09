package com.example.dindon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.theme.neuShadow

@Composable
fun NewButton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 6.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neuShadow(cornerRadius = cornerRadius, elevation = elevation)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        content()
    }
}
