package com.example.hostelfixapp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    thumbColor: Color = Color(0xFF6750A4).copy(alpha = 0.8f),
    width: Dp = 6.dp
): Modifier = this.drawWithContent {
    drawContent()
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo

    if (totalItems > 0 && visibleItems.isNotEmpty() && totalItems > visibleItems.size) {
        val firstVisible = visibleItems.firstOrNull()?.index ?: 0
        val viewportHeight = size.height

        val thumbHeight = maxOf((visibleItems.size.toFloat() / totalItems) * viewportHeight, 36.dp.toPx())
        val scrollRange = viewportHeight - thumbHeight
        val maxIndex = maxOf(totalItems - visibleItems.size, 1)
        val scrollProgress = (firstVisible.toFloat() / maxIndex).coerceIn(0f, 1f)
        val thumbTop = scrollProgress * scrollRange

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(size.width - width.toPx() - 4.dp.toPx(), thumbTop),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

@Composable
fun FilterRowScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color(0xFF6750A4)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        val maxVal = scrollState.maxValue
        val progress = if (maxVal > 0) {
            (scrollState.value.toFloat() / maxVal).coerceIn(0f, 1f)
        } else {
            0f
        }

        val thumbWidthRatio = 0.95f // 95% long scrollbar thumb

        Box(
            modifier = Modifier
                .fillMaxWidth(thumbWidthRatio)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    val trackWidth = size.width
                    val thumbW = trackWidth * thumbWidthRatio
                    val maxTranslate = trackWidth - thumbW
                    translationX = progress * maxTranslate
                }
                .background(thumbColor, RoundedCornerShape(4.dp))
        )
    }
}
