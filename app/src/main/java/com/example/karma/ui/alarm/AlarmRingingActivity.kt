package com.example.karma.ui.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.karma.KarmaApplication
import com.example.karma.data.local.entity.AlarmEntity
import com.example.karma.ui.components.pressFeedback
import com.example.karma.ui.theme.Gold
import kotlinx.coroutines.runBlocking

/**
 * 闹钟响铃全屏页（照系统闹钟）：
 * - 锁屏/亮屏显示，事件名 + 闹钟时间
 * - 贪睡 / 关闭 按钮（发广播给 AlarmReceiver 统一处理）
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 锁屏亮屏显示（API 27+ 用 setShowWhenLocked/setTurnScreenOn，低版本用 window flag）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        setContent {
            AlarmRingingScreen(
                alarmId = alarmId,
                onDismiss = {
                    sendAction(AlarmReceiver.ACTION_DISMISS, alarmId)
                    finish()
                },
                onSnooze = {
                    sendAction(AlarmReceiver.ACTION_SNOOZE, alarmId)
                    finish()
                },
            )
        }
    }

    private fun sendAction(action: String, alarmId: Long) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }
}

@Composable
private fun AlarmRingingScreen(
    alarmId: Long,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val context = LocalContext.current
    var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
    LaunchedEffect(alarmId) {
        alarm = runBlocking {
            (context.applicationContext as? KarmaApplication)
                ?.container?.repository?.getAlarmById(alarmId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a0a0f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = alarm?.eventName?.ifEmpty { "闹钟" } ?: "闹钟",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(12.dp))
            val timeText = alarm?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "--:--"
            Text(
                text = timeText,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.height(40.dp))
            val snoozeEnabled = (alarm?.snoozeMinutes ?: 0) > 0
            if (snoozeEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    RingButton(text = "稍后提醒", color = Color(0xFF4a90d9), onClick = onSnooze)
                    RingButton(text = "关闭", color = Color(0xFFff5252), onClick = onDismiss)
                }
            } else {
                RingButton(text = "关闭", color = Color(0xFFff5252), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun RingButton(text: String, color: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
            .pressFeedback(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = color,
        )
    }
}
