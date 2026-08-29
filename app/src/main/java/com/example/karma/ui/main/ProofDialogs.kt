package com.example.karma.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.model.ProofEngine
import com.example.karma.data.model.Rank
import com.example.karma.ui.components.ScrollPicker
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.TextMuted
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

/**
 * 被动自证询问弹窗：加分跨入正阶位时弹出。
 * 设置自证 / 取消（逃避，扣分退回）；点弹窗外忽略（不扣分不开始）。
 */
@Composable
fun PassiveProofPromptDialog(
    prompt: PassiveProofPrompt,
    onSetup: () -> Unit,
    onEscape: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("阶位自证", fontFamily = FontFamily.Serif, color = Gold) },
        text = {
            Column {
                Text(
                    "你已登上新的阶位，需要证明自己配得上它。",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "「设置自证」：以加分前的阶位为起点，整个时长内保持不降级，" +
                        "时长结束时阶位高于起点即成功；\n" +
                        "「逃避自证」：扣分退回上一阶位。",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSetup,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("设置自证", fontFamily = FontFamily.Serif) }
        },
        dismissButton = {
            TextButton(onClick = onEscape) {
                Text("逃避自证（扣分退回）", fontFamily = FontFamily.Serif, color = Color(0xFFff5252))
            }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

/**
 * 自证设置弹窗：时长（时:分 滚轮）+ 成功奖励分 + 失败惩罚分。
 * 时长必须覆盖下一个业力衰减时刻（衰减开启时）。
 */
@Composable
fun ProofSetupDialog(
    settings: KarmaSettingsEntity,
    setup: ProofSetup,
    onConfirm: (reward: Float, penalty: Float, durationMs: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedHour by remember { mutableStateOf(1) }
    var selectedMinute by remember { mutableStateOf(0) }
    var rewardText by remember { mutableStateOf("") }
    var penaltyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置自证", fontFamily = FontFamily.Serif, color = Gold) },
        text = {
            Column {
                Text(
                    if (setup.mode == ProofMode.ACTIVE) {
                        "规则：以当前阶位为起点，整个时长内保持不降级；" +
                            "时长结束时阶位高于起点即成功（恭喜登上结束时的阶位）"
                    } else {
                        "规则：以加分前的阶位为起点，整个时长内保持不降级；" +
                            "时长结束时阶位高于起点即成功（恭喜登上结束时的阶位）"
                    },
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))

                Text("时长（0~24 小时）", fontFamily = FontFamily.Serif, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScrollPicker(
                        range = 0..23,
                        selected = selectedHour,
                        onSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    ScrollPicker(
                        range = 0..59,
                        selected = selectedMinute,
                        onSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (settings.decayEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "业力衰减开启中：时长必须覆盖衰减时刻",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = TextMuted,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("成功奖励分", fontFamily = FontFamily.Serif, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it },
                    placeholder = { Text("如 5", fontFamily = FontFamily.Serif, color = TextMuted) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Gold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                Text("失败/放弃惩罚分", fontFamily = FontFamily.Serif, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = penaltyText,
                    onValueChange = { penaltyText = it },
                    placeholder = { Text("如 3", fontFamily = FontFamily.Serif, color = TextMuted) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Gold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationMs = (selectedHour * 60L + selectedMinute) * 60_000L
                    if (durationMs < 60_000L) {
                        android.widget.Toast.makeText(context, "时长至少 1 分钟", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (settings.decayEnabled) {
                        val minMs = ProofEngine.minDurationToCoverDecay(settings, System.currentTimeMillis())
                        if (durationMs < minMs) {
                            val needMin = (minMs / 60_000L).coerceAtLeast(1L)
                            android.widget.Toast.makeText(
                                context,
                                "时长过短：需覆盖业力衰减时间，至少 $needMin 分钟",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            return@Button
                        }
                    }
                    val reward = rewardText.toFloatOrNull()
                    val penalty = penaltyText.toFloatOrNull()
                    if (reward == null || penalty == null || reward < 0f || penalty < 0f) {
                        android.widget.Toast.makeText(
                            context,
                            "请输入有效的奖励与惩罚分数（≥0）",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        return@Button
                    }
                    onConfirm(reward, penalty, durationMs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("开始自证", fontFamily = FontFamily.Serif) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontFamily = FontFamily.Serif, color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

/** 自证结果弹窗（成功/失败）。 */
@Composable
fun ProofResultDialog(
    result: ProofResult,
    ranks: List<Rank>,
    successColor: Long,
    failColor: Long,
    onDismiss: () -> Unit,
) {
    val endName = ranks.find { it.level == result.targetRank }?.name ?: "新阶位"
    val mainColor = if (result.success) Color(successColor) else Color(failColor)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (result.success) "恭喜登临 ${endName}！" else "自证失败",
                fontFamily = FontFamily.Serif,
                color = mainColor,
            )
        },
        text = {
            Column {
                Text(
                    text = if (result.success) {
                        "你全程保持住了阶位，成功登临 $endName" +
                            if (result.reward > 0f) "，奖励 ${result.reward} 分" else ""
                    } else {
                        "未能通过自证" +
                            if (result.penalty > 0f) "，已扣除 ${result.penalty} 分" else ""
                    },
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = mainColor, contentColor = Color.Black),
            ) { Text("确定", fontFamily = FontFamily.Serif) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}
