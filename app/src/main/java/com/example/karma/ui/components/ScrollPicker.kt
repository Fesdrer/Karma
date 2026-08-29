package com.example.karma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.TextMuted
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 滑动选择器（滚轮）：两位数字显示（00~99），选中项居中。
 * 用于业力衰减的时/分选择等场景。
 */
@Composable
fun ScrollPicker(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseItems = range.toList()
    val baseSize = baseItems.size
    // 重复 3 份实现循环效果
    val items = remember(range) {
        buildList { repeat(3) { addAll(range.toList()) } }
    }
    val totalSize = items.size

    val itemHeight = 44.dp
    val visibleItems = 5
    val scope = rememberCoroutineScope()

    // 初始定位在中间副本（第 2 份），并让选中项居中
    val startIndex = baseSize + baseItems.indexOf(selected).coerceAtLeast(0) - visibleItems / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex.coerceAtLeast(0)
    )

    // 用 layoutInfo 找到视口正中间的项
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf 0
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }?.index?.coerceIn(0, totalSize - 1) ?: 0
        }
    }

    // 选中值通过取模归一化到 base 范围
    LaunchedEffect(centerItemIndex) {
        onSelected(baseItems[centerItemIndex % baseSize])
    }

    // 停止滑动后自动吸附：让最近项对齐到视口中心。
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    delay(60)
                    val info = listState.layoutInfo
                    if (info.visibleItemsInfo.isEmpty()) return@collect
                    val vc = info.viewportEndOffset / 2
                    val closest = info.visibleItemsInfo.minByOrNull { i ->
                        abs((i.offset + i.size / 2) - vc)
                    } ?: return@collect
                    val diff = closest.offset + closest.size / 2f - vc
                    if (abs(diff) > 4f) {
                        // animateScrollToItem 把目标放顶部；减 visibleItems/2 位置 → 目标落到第 3 位 = 中心
                        val snapFirst = (closest.index - visibleItems / 2)
                            .coerceIn(0, totalSize - 1)
                        listState.animateScrollToItem(snapFirst)
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItems)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 选中高亮条（始终保持在正中间）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(Gold.copy(alpha = 0.12f))
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, value ->
                val isCenter = index == centerItemIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            scope.launch { listState.animateScrollToItem(index) }
                            onSelected(baseItems[value % baseSize])
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format("%02d", value),
                        fontSize = if (isCenter) 22.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) Color.White else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
