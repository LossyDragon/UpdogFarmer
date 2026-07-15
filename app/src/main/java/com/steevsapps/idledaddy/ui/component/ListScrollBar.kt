package com.steevsapps.idledaddy.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = scrollbarImpl(
    isScrollInProgress = state.isScrollInProgress,
    layout = {
        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
            null
        } else {
            val firstVisibleItem = visibleItemsInfo.first()
            val estimatedRowSize =
                visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsInfo.size
            ScrollbarLayout(
                totalItemsCount = totalItemsCount,
                totalContentHeight = estimatedRowSize * totalItemsCount,
                scrolled = firstVisibleItem.index * estimatedRowSize - firstVisibleItem.offset,
                estimatedRowSize = estimatedRowSize,
                itemsPerRow = 1,
            )
        }
    },
    scrollToItem = state::scrollToItem,
    width = width,
    color = color,
)

@Composable
fun Modifier.scrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = scrollbarImpl(
    isScrollInProgress = state.isScrollInProgress,
    layout = {
        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
            null
        } else {
            val firstVisibleItem = visibleItemsInfo.first()
            val visibleRows = visibleItemsInfo.last().row - firstVisibleItem.row + 1
            val estimatedRowSize =
                visibleItemsInfo.sumOf { it.size.height }.toFloat() / visibleItemsInfo.size
            val itemsPerRow = (visibleItemsInfo.size.toFloat() / visibleRows)
                .roundToInt()
                .coerceAtLeast(1)
            val totalRows = ceil(totalItemsCount.toFloat() / itemsPerRow)
            ScrollbarLayout(
                totalItemsCount = totalItemsCount,
                totalContentHeight = estimatedRowSize * totalRows,
                scrolled = firstVisibleItem.row * estimatedRowSize - firstVisibleItem.offset.y,
                estimatedRowSize = estimatedRowSize,
                itemsPerRow = itemsPerRow,
            )
        }
    },
    scrollToItem = state::scrollToItem,
    width = width,
    color = color,
)

private class ScrollbarLayout(
    val totalItemsCount: Int,
    val totalContentHeight: Float,
    val scrolled: Float,
    val estimatedRowSize: Float,
    val itemsPerRow: Int,
)

@Composable
private fun Modifier.scrollbarImpl(
    isScrollInProgress: Boolean,
    layout: () -> ScrollbarLayout?,
    scrollToItem: suspend (index: Int, offset: Int) -> Unit,
    width: Dp,
    color: Color,
): Modifier {
    val alpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val currentLayout by rememberUpdatedState(layout)
    val currentScrollToItem by rememberUpdatedState(scrollToItem)

    LaunchedEffect(isScrollInProgress, dragging) {
        if (isScrollInProgress || dragging) {
            alpha.snapTo(1f)
        } else {
            delay(1.seconds)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
        }
    }

    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            if (alpha.value == 0f || down.position.x < size.width - 12.dp.toPx()) {
                return@awaitEachGesture
            }

            fun scrollTo(y: Float) {
                val info = currentLayout() ?: return
                if (info.totalContentHeight <= size.height) return
                val thumbHeight = size.height * (size.height / info.totalContentHeight)
                val fraction =
                    ((y - thumbHeight / 2) / (size.height - thumbHeight)).coerceIn(0f, 1f)
                val target = fraction * (info.totalContentHeight - size.height)
                val row = (target / info.estimatedRowSize).toInt()
                val index = (row * info.itemsPerRow).coerceIn(0, info.totalItemsCount - 1)
                val offset = (target - row * info.estimatedRowSize).toInt()
                scope.launch { currentScrollToItem(index, offset) }
            }

            dragging = true
            down.consume()
            scrollTo(down.position.y)
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                change.consume()
                scrollTo(change.position.y)
            }
            dragging = false
        }
    }.drawWithContent {
        drawContent()

        val info = layout()
        if (alpha.value > 0f && info != null && info.totalContentHeight > size.height) {
            val thumbHeight = size.height * (size.height / info.totalContentHeight)
            val scrollFraction =
                (info.scrolled / (info.totalContentHeight - size.height)).coerceIn(0f, 1f)
            val scrollOffset = scrollFraction * (size.height - thumbHeight)

            drawRoundRect(
                color = color.copy(alpha = color.alpha * alpha.value),
                topLeft = Offset(x = size.width - width.toPx(), y = scrollOffset),
                size = Size(width = width.toPx(), height = thumbHeight),
                cornerRadius = CornerRadius(x = width.toPx() / 2, y = width.toPx() / 2)
            )
        }
    }
}

/**
 * Preview
 */

@Preview
@Composable
private fun Preview() {
    val state = rememberLazyListState()
    IdleTheme {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .scrollbar(state),
        ) {
            items(100) { index ->
                Text(
                    text = "Item $index",
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun GridPreview() {
    val state = rememberLazyGridState()
    IdleTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .scrollbar(state),
        ) {
            items(100) { index ->
                Text(
                    text = "Item $index",
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}