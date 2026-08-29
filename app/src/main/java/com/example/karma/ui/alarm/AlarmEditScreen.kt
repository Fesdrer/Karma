package com.example.karma.ui.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.BackButton
import com.example.karma.ui.components.ScrollPicker
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.TextMuted
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

/**
 * 闹钟新建/编辑页（照手机闹钟）：
 * - 顶部大时间（点击弹出时:分滚轮）
 * - 行：重复（仅一次/每天/自定义周几）、事件（预设+自定义）、铃声（系统选择器）、震动、贪睡
 * - 底部保存；编辑已有闹钟时显示删除
 */
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: AlarmEditViewModel = viewModel(
        factory = AlarmEditViewModel.Factory(
            appContainer.repository,
            context.applicationContext as android.app.Application,
        )
    )
    val draft by viewModel.draft.collectAsState()
    val settings by appContainer.repository.settings.collectAsState(initial = null)

    LaunchedEffect(Unit) { viewModel.load(alarmId) }

    // 对话框开关
    var showTimePicker by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 系统铃声选择器
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let { viewModel.setRingtone(it.toString()) }
        }
    }

    val d = draft ?: return   // 数据未就绪不渲染（避免空草稿闪帧）

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackButton(onBack = onBack)
            Text(
                text = if (alarmId > 0) "编辑闹钟" else "新建闹钟",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(0.dp))
        }

        Spacer(Modifier.padding(top = 20.dp))

        // ===== 顶部大时间（点击弹滚轮） =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .background(Color(0xFF14141f))
                .clickable { showTimePicker = true }
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d:%02d", d.hour, d.minute),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
            )
        }

        Spacer(Modifier.padding(top = 16.dp))

        // ===== 设置行 =====
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingRow(title = "重复", value = AlarmScheduler.repeatText(d.repeatDays)) {
                showRepeatDialog = true
            }
            SettingRow(title = "事件", value = d.eventName.ifEmpty { "选择事件" }) {
                showEventDialog = true
            }
            SettingRow(
                title = "铃声",
                value = if (d.ringtoneUri.isEmpty()) "默认闹钟铃声" else "自选铃声",
            ) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声")
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        d.ringtoneUri.ifEmpty {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                        },
                    )
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                }
                ringtoneLauncher.launch(intent)
            }
            SwitchRow(title = "震动", checked = d.vibrate) { viewModel.setVibrate(it) }
            SettingRow(
                title = "贪睡",
                value = if (d.snoozeMinutes > 0) "${d.snoozeMinutes} 分钟" else "关闭",
            ) {
                showSnoozeDialog = true
            }
        }

        Spacer(Modifier.weight(1f))

        // ===== 保存 / 删除 =====
        Button(
            onClick = { viewModel.save(onDone = onBack) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text("保存", fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (alarmId > 0) {
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFff5252)),
            ) {
                Text("删除闹钟", fontFamily = FontFamily.Serif, fontSize = 15.sp)
            }
        }
    }

    // ===== 对话框 =====
    if (showTimePicker) {
        AlarmTimePickerDialog(
            initialHour = d.hour,
            initialMinute = d.minute,
            onConfirm = { h, m ->
                viewModel.updateHour(h)
                viewModel.updateMinute(m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
    if (showRepeatDialog) {
        RepeatDialog(
            initial = d.repeatDays,
            onConfirm = { days ->
                viewModel.setRepeatDays(days)
                showRepeatDialog = false
            },
            onDismiss = { showRepeatDialog = false },
        )
    }
    if (showEventDialog) {
        EventSelectDialog(
            goodDeeds = settings?.goodDeedPresets ?: emptyList(),
            badDeeds = settings?.badDeedPresets ?: emptyList(),
            goodResults = settings?.goodResultPresets ?: emptyList(),
            current = d.eventName,
            onConfirm = { name ->
                viewModel.setEventName(name)
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }
    if (showSnoozeDialog) {
        SnoozeDialog(
            initialMinutes = d.snoozeMinutes,
            onConfirm = { minutes ->
                viewModel.setSnoozeMinutes(minutes)
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false },
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除闹钟", color = Gold) },
            text = { Text("确定删除这个闹钟吗？", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAndDone(onDone = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFff5252)),
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1A1A1A),
        )
    }
}

// ===== 设置行组件 =====

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .background(Color(0xFF14141f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            color = if (value.isEmpty()) TextMuted else Gold,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
        Text("  ›", fontSize = 16.sp, color = TextMuted)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .background(Color(0xFF14141f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Gold,
                checkedTrackColor = Gold.copy(alpha = 0.3f),
            ),
        )
    }
}

// ===== 时间选择对话框（时:分 滚轮，同业力衰减） =====

@Composable
private fun AlarmTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", fontFamily = FontFamily.Serif, color = Gold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedHour, selectedMinute)
                    onDismiss()
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

// ===== 重复对话框（仅一次 / 每天 / 自定义周几多选） =====

private val WEEK_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
private fun RepeatDialog(
    initial: List<Int>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var days by remember { mutableStateOf(initial.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复", color = Gold) },
        text = {
            Column {
                // 快捷：仅一次（清空）/ 每天（全选）
                Row {
                    TextButton(onClick = { days = emptySet() }) {
                        Text("仅一次", color = if (days.isEmpty()) Gold else TextSecondary)
                    }
                    TextButton(onClick = { days = (1..7).toSet() }) {
                        Text("每天", color = if (days.size == 7) Gold else TextSecondary)
                    }
                }
                WEEK_NAMES.forEachIndexed { index, name ->
                    val day = index + 1
                    val checked = day in days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                days = if (checked) days - day else days + day
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                days = if (isChecked) days + day else days - day
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Gold,
                                uncheckedColor = BorderSubtle,
                                checkmarkColor = Color.Black,
                            ),
                        )
                        Text(name, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(days.sorted()) }) {
                Text("确定", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

// ===== 事件选择对话框（善业/恶业/善果预设 + 自定义） =====

@Composable
private fun EventSelectDialog(
    goodDeeds: List<String>,
    badDeeds: List<String>,
    goodResults: List<String>,
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var customText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择事件", color = Gold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                EventSectionHeader("善业", Color(0xFF69f0ae))
                goodDeeds.forEach { name ->
                    EventOption(name, name == current) { onConfirm(name) }
                }
                EventSectionHeader("恶业", Color(0xFFff5252))
                badDeeds.forEach { name ->
                    EventOption(name, name == current) { onConfirm(name) }
                }
                EventSectionHeader("善果", Color(0xFFffd700))
                goodResults.forEach { name ->
                    EventOption(name, name == current) { onConfirm(name) }
                }

                Spacer(Modifier.padding(top = 10.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("自定义事件...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                    ),
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
            TextButton(
                onClick = {
                    val custom = customText.trim()
                    onConfirm(custom.ifEmpty { current })
                },
                enabled = customText.isNotBlank(),
            ) {
                Text("确定自定义", color = if (customText.isNotBlank()) Gold else TextMuted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

@Composable
private fun EventSectionHeader(title: String, color: Color) {
    Text(
        text = "── $title ──",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun EventOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Gold.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = if (selected) Gold else TextPrimary,
        )
    }
}

// ===== 贪睡时长对话框 =====

private val SNOOZE_OPTIONS = listOf(0, 5, 10, 15, 20, 30)

@Composable
private fun SnoozeDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by remember { mutableStateOf(initialMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("贪睡", color = Gold) },
        text = {
            Column {
                SNOOZE_OPTIONS.forEach { option ->
                    val label = if (option == 0) "关闭" else "$option 分钟"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                minutes = option
                                onConfirm(option)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = if (minutes == option) Gold else TextPrimary,
                            fontWeight = if (minutes == option) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (minutes == option) {
                            Text("✓", color = Gold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}
