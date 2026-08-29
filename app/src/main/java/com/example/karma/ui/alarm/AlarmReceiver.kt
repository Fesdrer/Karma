package com.example.karma.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * 闹钟广播接收器：
 * - ACTION_RING：到点响铃 → 启动前台响铃服务
 * - ACTION_DISMISS / ACTION_SNOOZE：通知按钮或响铃页操作 → 处理关闭/贪睡
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        when (intent.action) {
            ACTION_RING -> {
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    action = AlarmRingingService.ACTION_RING
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            ACTION_DISMISS -> AlarmActions.dismiss(context, alarmId)
            ACTION_SNOOZE -> AlarmActions.snooze(context, alarmId)
        }
    }

    companion object {
        const val ACTION_RING = "com.example.karma.alarm.RING"
        const val ACTION_DISMISS = "com.example.karma.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.example.karma.alarm.SNOOZE"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}
