package com.example.karma.ui.bet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.data.local.entity.Bet
import com.example.karma.data.repository.KarmaRepository
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.BackButton
import com.example.karma.ui.components.DialogEntranceContainer
import com.example.karma.ui.components.ScrollPicker
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.ScoreBtnBg
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary
import java.util.Calendar

/** ✔ 成功（绿色系） */
private val SuccessGreen = Color(0xFF4CAF50)

/** × 失败（红色系） */
private val FailureRed = Color(0xFFe53935)

@Composable
fun BetScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BetViewModel = viewModel(
        factory = BetViewModel.Factory(appContainer.repository)
    )
    val bets by viewModel.bets.collectAsState()

    var showNewBetDialog by remember { mutableStateOf(false) }
    var detailBet by remember { mutableStateOf<Bet?>(null) }

    // Column 始终在组合树中，确保 NavHost 的 fadeIn 有可见目标来执行渐变动画
    // 数据就绪前先渲染标题栏，数据就绪后渲染完整内容
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        // Row 1: Back + Title (left) | ＋ (right)
        // 始终渲染，为 fadeIn 提供可见内容
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack = onBack)

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "誓约",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFffd700),
                    letterSpacing = 2.sp,
                )
            }

            // ＋ 新建誓约
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                    .clickable { showNewBetDialog = true }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Text("＋", fontSize = 16.sp, color = Color(0xFFa0c4ff))
            }
        }

        // 数据就绪后再渲染列表区（bets == null 不渲染，避免首帧闪"空列表"）
        val list = bets
        if (list != null) {
            Spacer(Modifier.height(8.dp))

            if (list.isEmpty()) {
                // 空状态：居中提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无誓约，点右上角 + 立下你的誓言",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    list.forEach { bet ->
                        BetRow(
                            bet = bet,
                            onClick = { detailBet = bet },
                            onSuccess = { viewModel.resolveBet(bet, true) },
                            onFailure = { viewModel.resolveBet(bet, false) },
                        )
                    }
                }
            }
        }
    }

    // 新建誓约表单弹窗
    if (showNewBetDialog) {
        NewBetDialog(
            onDismiss = { showNewBetDialog = false },
            onConfirm = { content, deadlineAt, success, failure ->
                viewModel.addBet(content, deadlineAt, success, failure)
                showNewBetDialog = false
            },
        )
    }

    // 详情弹窗
    if (detailBet != null) {
        BetDetailDialog(
            bet = detailBet!!,
            onDismiss = { detailBet = null },
        )
    }
}

/** 一条进行中的誓约行：内容(省略) + 期限 + ✔/× 两个独立按钮。 */
@Composable
private fun BetRow(
    bet: Bet,
    onClick: () -> Unit,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bet.content,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = KarmaRepository.formatDateTime(bet.deadlineAt),
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1,
        )

        Spacer(Modifier.width(8.dp))

        // ✔ 成功（独立 clickable，不与整行「看详情」冲突）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SuccessGreen.copy(alpha = 0.15f))
                .border(1.dp, SuccessGreen, RoundedCornerShape(6.dp))
                .clickable(onClick = onSuccess)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("✔", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
        }

        Spacer(Modifier.width(6.dp))

        // × 失败（独立 clickable）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(FailureRed.copy(alpha = 0.15f))
                .border(1.dp, FailureRed, RoundedCornerShape(6.dp))
                .clickable(onClick = onFailure)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FailureRed)
        }
    }
}

/** 新建誓约表单弹窗：内容/时间/两个分数全合法前「确定」禁用。 */
@Composable
private fun NewBetDialog(
    onDismiss: () -> Unit,
    onConfirm: (content: String, deadlineAt: Long, success: Float, failure: Float) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    // v4.3：终止时间改为滚轮选择，默认当前时间
    var deadlineAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var successText by remember { mutableStateOf("") }
    var failureText by remember { mutableStateOf("") }

    val success = successText.toFloatOrNull()
    val failure = failureText.toFloatOrNull()
    val valid = content.isNotBlank() &&
        success != null && success > 0f &&
        failure != null && failure > 0f

    DialogEntranceContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "立下誓约",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(16.dp))

            FormField(
                label = "对赌内容",
                value = content,
                onValueChange = { content = it },
                placeholder = "如：今天必须运动",
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            // v4.3：终止时间（点击弹滚轮选择，默认当前时间）
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "终止时间",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScoreBtnBg)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = KarmaRepository.formatDateTime(deadlineAt),
                        fontSize = 16.sp,
                        color = Color(0xFFe0e0e0),
                        modifier = Modifier.weight(1f),
                    )
                    Text("修改", fontSize = 13.sp, color = Color(0xFFa0c4ff))
                }
                Spacer(Modifier.height(4.dp))
                Text("到期后回到首页会弹出提示，选择是否完成", fontSize = 11.sp, color = Color(0xFF666666))
            }
            Spacer(Modifier.height(12.dp))

            FormField(
                label = "成功加分",
                value = successText,
                onValueChange = { successText = it },
                placeholder = "填正数",
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            FormField(
                label = "失败减分",
                value = failureText,
                onValueChange = { failureText = it },
                placeholder = "填正数，失败时扣除",
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color(0xFF888888),
                    ),
                    modifier = Modifier.height(44.dp),
                ) {
                    Text("取消", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val s = successText.toFloatOrNull()
                        val f = failureText.toFloatOrNull()
                        if (s != null && f != null) {
                            onConfirm(content.trim(), deadlineAt, s, f)
                        }
                    },
                    enabled = valid,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (valid) Color(0xFFb8860b) else Color(0xFF333333),
                        contentColor = if (valid) Color.White else Color(0xFF666666),
                    ),
                    modifier = Modifier.height(44.dp),
                ) {
                    Text("确定", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 终止时间选择（v4.3）
    if (showTimePicker) {
        BetTimePickerDialog(
            initialAt = deadlineAt,
            onConfirm = { deadlineAt = it },
            onDismiss = { showTimePicker = false },
        )
    }
}

/**
 * 终止时间选择弹窗（v4.3）：第一行 年/月/日，第二行 时/分/秒（仿业力衰减的滚轮设置）。
 * 年月日不联动（日固定 1~31），确定时校验合法性——如 2 月 30 日弹错误提示不关闭。
 */
@Composable
private fun BetTimePickerDialog(
    initialAt: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val cal = remember(initialAt) { Calendar.getInstance().apply { timeInMillis = initialAt } }
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }
    var second by remember { mutableIntStateOf(cal.get(Calendar.SECOND)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择终止时间", fontFamily = FontFamily.Serif, color = Gold) },
        text = {
            Column {
                // 第一行：年 / 月 / 日
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScrollPicker(
                        range = 2024..2100,
                        selected = year,
                        onSelected = { year = it },
                        modifier = Modifier.weight(1.4f),
                    )
                    PickerUnit("年")
                    ScrollPicker(
                        range = 1..12,
                        selected = month,
                        onSelected = { month = it },
                        modifier = Modifier.weight(1f),
                    )
                    PickerUnit("月")
                    ScrollPicker(
                        range = 1..31,
                        selected = day,
                        onSelected = { day = it },
                        modifier = Modifier.weight(1f),
                    )
                    PickerUnit("日")
                }
                Spacer(Modifier.height(8.dp))
                // 第二行：时 / 分 / 秒
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScrollPicker(
                        range = 0..23,
                        selected = hour,
                        onSelected = { hour = it },
                        modifier = Modifier.weight(1f),
                    )
                    PickerUnit("时")
                    ScrollPicker(
                        range = 0..59,
                        selected = minute,
                        onSelected = { minute = it },
                        modifier = Modifier.weight(1f),
                    )
                    PickerUnit("分")
                    ScrollPicker(
                        range = 0..59,
                        selected = second,
                        onSelected = { second = it },
                        modifier = Modifier.weight(1f),
                    )
                    PickerUnit("秒")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxDay = daysInMonth(year, month)
                    if (day > maxDay) {
                        Toast.makeText(
                            context,
                            "${year}年${month}月没有${day}日，请重新选择",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month - 1)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, second)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(picked.timeInMillis)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

/** 滚轮右侧的单位文字（年/月/日 时/分/秒）。 */
@Composable
private fun PickerUnit(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** 该年月的天数（用于校验 2 月 30 日等非法日期）。 */
private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    }
}

/** 表单字段：标签 + OutlinedTextField（样式仿祈福表单）。 */
@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF888888),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF666666)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            textStyle = TextStyle(color = Color(0xFFe0e0e0), fontSize = 16.sp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFffd700),
                unfocusedBorderColor = BorderSubtle,
                cursorColor = Color(0xFFffd700),
                focusedContainerColor = ScoreBtnBg,
                unfocusedContainerColor = ScoreBtnBg,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 详情弹窗：显示誓约四项完整内容 + 立誓时间。 */
@Composable
private fun BetDetailDialog(
    bet: Bet,
    onDismiss: () -> Unit,
) {
    DialogEntranceContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "誓约详情",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(16.dp))

            DetailRow("内容", bet.content)
            Spacer(Modifier.height(10.dp))
            DetailRow("终止时间", KarmaRepository.formatDateTime(bet.deadlineAt))
            Spacer(Modifier.height(10.dp))
            DetailRow("成功加分", "+${formatPoints(bet.successPoints)}")
            Spacer(Modifier.height(10.dp))
            DetailRow("失败减分", "-${formatPoints(bet.failurePoints)}")
            Spacer(Modifier.height(10.dp))
            DetailRow("立誓时间", KarmaRepository.formatDate(bet.createdAt))

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFb8860b),
                    contentColor = Color.White,
                ),
                modifier = Modifier.height(44.dp),
            ) {
                Text("确定", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 详情行：标签 + 内容（内容可换行显示全文）。 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF888888),
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFFe0e0e0),
            modifier = Modifier.weight(1f),
        )
    }
}

/** 誓约分数格式化：整数去小数点（3 而非 3.0），小数保留原样（3.5）。 */
private fun formatPoints(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else value.toString()
}
