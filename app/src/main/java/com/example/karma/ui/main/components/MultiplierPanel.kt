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
import com.example.karma.ui.components.pressFeedback
import com.example.karma.ui.theme.Gold
import kotlin.math.abs

/**
 * 主页面左栏乘法区：一组乘法按钮（单列竖排，可滚动）。
 * 点击后当前分数 × 该值（结果向 0.5 四舍五入，逻辑在 MainViewModel.multiplyScore）。
 */
@Composable
fun MultiplierPanel(
    multipliers: List<Float>,
    onMultiply: (Float) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                multipliers.forEach { m ->
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1A1A2E))
                            .border(1.dp, Color(0xFFb8860b).copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                            .pressFeedback(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                            ) { onMultiply(m) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = formatMultiplierLabel(m),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFffd700),
                        )
                    }
                }
            }
        }
    }
}

/** 乘法按钮标签：1/3、2/3、1/4、1/2、3/4 显示分数，其余整数去点、小数最多两位。 */
private fun formatMultiplierLabel(v: Float): String {
    val frac = when {
        isNear(v, 1f / 3f) -> "1/3"
        isNear(v, 2f / 3f) -> "2/3"
        isNear(v, 1f / 4f) -> "1/4"
        isNear(v, 1f / 2f) -> "1/2"
        isNear(v, 3f / 4f) -> "3/4"
        else -> null
    }
    if (frac != null) return "×$frac"
    return if (v % 1f == 0f) {
        "×${v.toInt()}"
    } else {
        "×" + String.format("%.2f", v).trimEnd('0').trimEnd('.')
    }
}

private fun isNear(v: Float, target: Float): Boolean = abs(v - target) < 0.001f
