package com.example.karma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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

/** 滚轮一屏显示的项数（奇数，正中间那一项即选中项）。 */
private const val PICKER_VISIBLE_ITEMS = 5

/**
 * 把离视口中心最近的那一项吸附到正中间。
 *
 * @param animate true 用动画吸附（滑动/视口变化后），false 直接定位（首次进入，避免可见跳动）
 * @return false 表示**布局尚未就绪**（`visibleItemsInfo` 为空），调用方需要稍后重试。
 *
 * 返回布尔值是这次修复的关键：原来布局没就绪时直接 `return@collect`，
 * 而 `snapshotFlow` 只在值**变化**时发射，这一次吸附被跳过之后就再也不会补做，
 * 表现就是「有时滚轮停在半格、不居中」。
 */
private suspend fun snapPickerToCenter(
    listState: LazyListState,
    visibleItems: Int,
    totalSize: Int,
    animate: Boolean,
): Boolean {
    val info = listState.layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return false
    val viewportCenter = info.viewportEndOffset / 2
    val closest = info.visibleItemsInfo.minByOrNull { i ->
        abs((i.offset + i.size / 2) - viewportCenter)
    } ?: return false
    val diff = closest.offset + closest.size / 2f - viewportCenter
    if (abs(diff) > 4f) {
        // animateScrollToItem/scrollToItem 把目标项放**顶部**；减 visibleItems/2 后
        // 目标落到第 3 位 = 正中间（一屏 5 项时即 index-2 置顶）。
        val snapFirst = (closest.index - visibleItems / 2).coerceIn(0, totalSize - 1)
        if (animate) listState.animateScrollToItem(snapFirst) else listState.scrollToItem(snapFirst)
    }
    return true
}

/**
 * 滑动选择器（滚轮）：两位数字显示（00~99），选中项居中。
 * 用于业力衰减的时/分选择、誓约终止时间、自证时长等场景。
 *
 * 居中相关的四个坑（v4.4 修复）：
 * 1. 布局未就绪时那次吸附被跳过且不再重试 → 改为返回布尔值 + 短重试。
 * 2. 弹窗里输入框弹出/收起软键盘会改变视口高度，此时并没有"滚动"事件，
 *    原来不会重新吸附 → 把视口高度也纳入监听，变化后重新吸附。
 * 3. 点击某一项时 `animateScrollToItem(index)` 把该项放到顶部，真正居中的是 index+2，
 *    点谁不居中谁 → 改为滚动到 index - visibleItems/2，让被点的那项居中。
 * 4. 布局未就绪时 `centerItemIndex` 兜底为 0，会把"第一项"当成选中值发射出去
 *    （年份滚轮会被莫名改成 2024）→ 未就绪时返回 -1 且不发射。
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
    val visibleItems = PICKER_VISIBLE_ITEMS
    val scope = rememberCoroutineScope()

    // 初始定位在中间副本（第 2 份），并让选中项居中
    val startIndex = baseSize + baseItems.indexOf(selected).coerceAtLeast(0) - visibleItems / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex.coerceAtLeast(0)
    )

    // 用 layoutInfo 找到视口正中间的项；布局未就绪返回 -1，避免把 0 当成选中值
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf -1
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }?.index?.coerceIn(0, totalSize - 1) ?: -1
        }
    }

    // 选中值通过取模归一化到 base 范围（布局就绪后才发射）
    LaunchedEffect(centerItemIndex) {
        val idx = centerItemIndex
        if (idx >= 0) onSelected(baseItems[idx % baseSize])
    }

    // 停止滑动 / 视口尺寸变化后自动吸附：让最近项对齐到视口中心。
    // 监听 `viewportEndOffset` 是为了覆盖"软键盘弹出收起导致弹窗高度变化"这种没有滚动事件的场景。
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress to listState.layoutInfo.viewportEndOffset }
            .collect { (scrolling, _) ->
                if (scrolling) return@collect
                delay(60)
                // 布局未就绪就短重试（弹窗刚出现、键盘刚收起时会出现），
                // 最多约 300ms；仍失败则放弃，等下一次事件再吸附。
                var tries = 0
                while (tries++ < 20) {
                    if (snapPickerToCenter(listState, visibleItems, totalSize, animate = true)) break
                    delay(16)
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
                            // 让**被点的这一项**落到正中间：滚动到 index - visibleItems/2
                            // （原来滚到 index，被点的项停在顶部，居中变成 index+2）
                            scope.launch {
                                val target = (index - visibleItems / 2).coerceIn(0, totalSize - 1)
                                listState.animateScrollToItem(target)
                            }
                            // 用下标取值，不用 value 取模：range 不以 0 开头时（如年份 2024..2100）
                            // `value % baseSize` 会算出一个完全无关的项。
                            onSelected(baseItems[index % baseSize])
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