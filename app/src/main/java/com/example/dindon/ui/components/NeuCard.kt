package com.example.dindon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dindon.ui.theme.Neu
import com.example.dindon.ui.theme.neuShadow

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .neuShadow(cornerRadius = cornerRadius, elevation = elevation)
            .clip(shape)
            .background(Neu.bg)
            .border(1.dp, Neu.outline, shape) // ✅ тонкий контур “собирает форму”
            .padding(contentPadding)
    ) {
        content()
    }
}
