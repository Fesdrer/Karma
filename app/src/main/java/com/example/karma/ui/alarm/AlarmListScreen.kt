package com.example.karma.ui.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.data.local.entity.AlarmEntity
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.BackButton
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.TextMuted

/**
 * 闹钟列表页（照手机闹钟）：
 * - 右上 + 新建，点击条目进入编辑
 * - 每项：时间（大字）+ 事件名 + 重复描述 + 开关
 * - 首次进入请求通知权限（Android 13+）
 */
@Composable
fun AlarmListScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onNewAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModel.Factory(
            appContainer.repository,
            context.applicationContext as android.app.Application,
        )
    )
    val alarms by viewModel.alarms.collectAsState()

    // 通知权限（Android 13+）：闹钟响铃依赖通知/全屏提醒
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户选择即可，未授权时响铃仅前台显示 */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                text = "闹钟",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 2.sp,
            )
            TextButton(onClick = onNewAlarm) {
                Text("+ 新建", fontSize = 16.sp, color = Gold, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.padding(top = 12.dp))

        when (val list = alarms) {
            null -> {
                // 数据未就绪：不渲染，避免空列表首帧闪现
            }
            else -> {
                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "暂无闹钟\n点右上角「+ 新建」添加",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { alarm ->
                            AlarmRow(
                                alarm = alarm,
                                onToggle = { enabled -> viewModel.toggleEnabled(alarm, enabled) },
                                onClick = { onEditAlarm(alarm.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .background(Color(0xFF14141f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = if (alarm.enabled) Color.White else Color(0xFF666666),
            )
            Spacer(Modifier.padding(top = 2.dp))
            Text(
                text = (alarm.eventName.ifEmpty { "无事件" }) +
                    "  ·  " + AlarmScheduler.repeatText(alarm.repeatDays),
                fontSize = 12.sp,
                color = if (alarm.enabled) TextMuted else Color(0xFF555555),
                maxLines = 1,
            )
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Gold,
                checkedTrackColor = Gold.copy(alpha = 0.3f),
            ),
        )
    }
}
