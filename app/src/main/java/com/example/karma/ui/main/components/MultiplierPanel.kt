package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.data.model.Fraction
import com.example.karma.ui.components.pressFeedback
import com.example.karma.ui.theme.Gold

/**
 * 主页面左栏乘法区：一组乘法按钮（单列竖排，可滚动）+ 底部「确定」。
 *
 * v4.1 起为「乘法分配律」交互：
 * - 点击乘数仅切换选中状态（分数不变），选中按钮金色高亮；不能重复选中同一乘数，再按一次取消。
 * - 底部「确定」一次性应用：新分数 = 当前分数 × Σ(选中乘数)，向 0.5 四舍五入（逻辑在 MainViewModel.confirmMultipliers）。
 * - 按钮标签始终显示分数形式（×1/3、×1/7、×1.5、×2）。
 */
@Composable
fun MultiplierPanel(
    multipliers: List<Fraction>,
    selectedIndices: Set<Int>,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Text(
            text = "乘法",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFffd700),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        if (multipliers.isEmpty()) {
            Text(
                text = "暂无乘法按钮\n（设置中添加）",
                fontSize = 9.sp,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                // 按钮组上下居中；按钮过多超出高度时仍从顶部排列、可滚动
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            ) {
                multipliers.forEachIndexed { index, m ->
                    val selected = index in selectedIndices
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) Color(0xFFffd700) else Color(0xFF1A1A2E))
                            .border(
                                1.dp,
                                if (selected) Color(0xFFffd700) else Color(0xFFb8860b).copy(alpha = 0.5f),
                                RoundedCornerShape(7.dp),
                            )
                            .pressFeedback(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                            ) { onToggle(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "×" + m.format(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color(0xFF1A1A2E) else Color(0xFFffd700),
                        )
                    }
                }
            }
        }

        // ===== 已选乘数提示 =====
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (selectedIndices.isEmpty()) {
                "点击选择乘数"
            } else {
                "已选：" + selectedIndices.sorted().joinToString(" ") { i ->
                    "×" + (multipliers.getOrNull(i)?.format() ?: "?")
                }
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedIndices.isEmpty()) Color(0xFF666666) else Color(0xFFffd700),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Spacer(Modifier.height(4.dp))

        // ===== 确定按钮（无选中时禁用）=====
        val confirmInteraction = remember { MutableInteractionSource() }
        val canConfirm = selectedIndices.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (canConfirm) Color(0xFFb8860b) else Color(0xFF2a2a3a))
                .pressFeedback(confirmInteraction)
                .clickable(
                    enabled = canConfirm,
                    interactionSource = confirmInteraction,
                    indication = null,
                ) { onConfirm() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "确定",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (canConfirm) Color.White else Color(0xFF777777),
            )
        }
    }
}
