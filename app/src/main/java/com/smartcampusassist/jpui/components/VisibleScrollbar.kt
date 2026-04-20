package com.smartcampusassist.jpui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.max
import kotlin.math.roundToInt

private val DropdownMenuItemHeight = 48.dp

@Stable
fun Modifier.visibleLazyColumnScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 36.dp,
    endPadding: Dp = 4.dp,
    thumbColor: Color = Color.Black.copy(alpha = 0.45f),
    trackColor: Color = Color.Black.copy(alpha = 0.12f)
): Modifier = composed {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val minThumbHeightPx = with(density) { minThumbHeight.toPx() }
    val endPaddingPx = with(density) { endPadding.toPx() }

    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@drawWithContent

        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        if (viewportHeight <= 0f) return@drawWithContent

        val averageItemHeight = visibleItems.map { it.size }.average().toFloat()
        if (averageItemHeight <= 0f) return@drawWithContent

        val totalContentHeight = averageItemHeight * layoutInfo.totalItemsCount +
            layoutInfo.beforeContentPadding +
            layoutInfo.afterContentPadding

        if (totalContentHeight <= viewportHeight) return@drawWithContent

        val estimatedScrollOffset =
            state.firstVisibleItemIndex * averageItemHeight + state.firstVisibleItemScrollOffset
        val thumbHeight = max(viewportHeight * viewportHeight / totalContentHeight, minThumbHeightPx)
        val maxScrollOffset = max(totalContentHeight - viewportHeight, 1f)
        val scrollProgress = (estimatedScrollOffset / maxScrollOffset).coerceIn(0f, 1f)
        val thumbOffsetY = (viewportHeight - thumbHeight) * scrollProgress

        val left = size.width - widthPx - endPaddingPx
        val radius = CornerRadius(widthPx / 2f, widthPx / 2f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, 0f),
            size = Size(widthPx, viewportHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(left, thumbOffsetY),
            size = Size(widthPx, thumbHeight),
            cornerRadius = radius
        )
    }
}

@Stable
fun Modifier.visibleVerticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 36.dp,
    endPadding: Dp = 4.dp,
    thumbColor: Color = Color.Black.copy(alpha = 0.45f),
    trackColor: Color = Color.Black.copy(alpha = 0.12f)
): Modifier = composed {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val minThumbHeightPx = with(density) { minThumbHeight.toPx() }
    val endPaddingPx = with(density) { endPadding.toPx() }

    drawWithContent {
        drawContent()

        val viewportHeight = size.height
        if (viewportHeight <= 0f) return@drawWithContent

        val maxScroll = state.maxValue.toFloat()
        if (maxScroll <= 0f) return@drawWithContent

        val totalContentHeight = viewportHeight + maxScroll
        val thumbHeight = max(viewportHeight * viewportHeight / totalContentHeight, minThumbHeightPx)
        val scrollProgress = (state.value / maxScroll).coerceIn(0f, 1f)
        val thumbOffsetY = (viewportHeight - thumbHeight) * scrollProgress

        val left = size.width - widthPx - endPaddingPx
        val radius = CornerRadius(widthPx / 2f, widthPx / 2f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, 0f),
            size = Size(widthPx, viewportHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(left, thumbOffsetY),
            size = Size(widthPx, thumbHeight),
            cornerRadius = radius
        )
    }
}

@Composable
fun <T> ScrollableDropdownMenuContent(
    items: List<T>,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 3,
    itemHeight: Dp = DropdownMenuItemHeight,
    itemContent: @Composable (T) -> Unit
) {
    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val width = 4.dp
    val minThumbHeight = 18.dp
    val endPadding = 2.dp
    val thumbColor = Color.Black.copy(alpha = 0.48f)
    val trackColor = Color.Black.copy(alpha = 0.16f)

    val maxScroll = scrollState.maxValue.toFloat()
    val viewportHeight = viewportHeightPx.toFloat()
    val totalContentHeight = viewportHeight + maxScroll
    val thumbHeightPx = if (maxScroll > 0f && viewportHeight > 0f) {
        max(
            viewportHeight * viewportHeight / totalContentHeight,
            with(density) { minThumbHeight.toPx() }
        )
    } else {
        0f
    }
    val thumbOffsetYPx = if (maxScroll > 0f && viewportHeight > thumbHeightPx) {
        ((viewportHeight - thumbHeightPx) * (scrollState.value / maxScroll).coerceIn(0f, 1f))
    } else {
        0f
    }
    val viewportHeightDp: Dp = with(density) { viewportHeightPx.toDp() }
    val thumbHeightDp: Dp = with(density) { thumbHeightPx.toDp() }

    Box(
        modifier = modifier
            .heightIn(max = itemHeight * maxVisibleItems)
            .onSizeChanged { viewportHeightPx = it.height }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            items.forEach { item ->
                itemContent(item)
            }
        }

        if (maxScroll > 0f && viewportHeightPx > 0 && thumbHeightPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = endPadding)
                    .width(width)
                    .height(viewportHeightDp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(trackColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = endPadding)
                    .offset { IntOffset(x = 0, y = thumbOffsetYPx.roundToInt()) }
                    .width(width)
                    .height(thumbHeightDp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(thumbColor)
            )
        }
    }
}
