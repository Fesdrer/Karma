package com.example.karma.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    score: Float,
    event: String,
    appContainer: AppContainer,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: TimerViewModel = viewModel(
        factory = TimerViewModel.Factory(appContainer.repository, score, event)
    )

    val timerState by TimerService.timerState.collectAsState()

    // 本地时间刷新（TimerState 只在状态切换时更新，需要本地 tick 驱动 UI 刷新）
    var displayMs by remember { mutableStateOf(0L) }
    LaunchedEffect(timerState.status) {
        while (timerState.status == TimerStatus.RUNNING) {
            displayMs = timerState.currentElapsedMs()
            delay(200)
        }
        // 暂停/停止时显示最终值
        displayMs = timerState.currentElapsedMs()
    }

    // 请求通知权限（Android 13+）
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 前台 Service 仍可运行，只是通知可能被隐藏 */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 启动计时服务
    LaunchedEffect(Unit) {
        TimerService.start(context, score, event)
    }

    val isRunning = timerState.status == TimerStatus.RUNNING
    val isPaused = timerState.status == TimerStatus.PAUSED
    val isActive = isRunning || isPaused

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // 时间显示
            Text(
                text = TimerService.formatTime(displayMs),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700), // 金色
            )

            Spacer(Modifier.height(24.dp))

            // 事件
            Text(
                text = event,
                fontSize = 16.sp,
                color = Color(0xFFaaaaaa),
            )

            // 分数
            Text(
                text = if (score >= 0) "+${score}" else "${score}",
                fontSize = 14.sp,
                color = if (score >= 0) Color(0xFF69f0ae) else Color(0xFFff5252),
            )

            Spacer(Modifier.weight(1f))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 暂停 / 继续 按钮
                Button(
                    onClick = {
                        if (isRunning) {
                            TimerService.pause(context)
                        } else if (isPaused) {
                            TimerService.resume(context)
                        }
                    },
                    enabled = isActive,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4a90d9),
                        disabledContainerColor = Color(0xFF222244),
                    ),
                ) {
                    Text(
                        text = if (isRunning) "暂停" else "继续",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else Color(0xFF555555),
                    )
                }

                // 停止 按钮
                Button(
                    onClick = {
                        viewModel.onStop(displayMs) { _ ->
                            TimerService.stop(context)
                            onComplete()
                        }
                    },
                    enabled = isActive,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFd32f2f),
                        disabledContainerColor = Color(0xFF222244),
                    ),
                ) {
                    Text(
                        text = "停止",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else Color(0xFF555555),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
