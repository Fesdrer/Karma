package com.example.karma.ui.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.karma.R
import com.example.karma.data.local.entity.AlarmEntity

/**
 * 闹钟通知：响铃全屏通知（贪睡/关闭按钮），随闹钟 id 区分通知 id。
 */
object AlarmNotifications {

    const val CHANNEL_ID = "karma_alarm"
    const val CHANNEL_NAME = "闹钟"
    const val NOTIFICATION_ID = 2000

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,   // 闹钟需要 HIGH：全屏/横幅提醒
            ).apply {
                description = "闹钟响铃提醒"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** 响铃通知：全屏 intent 指向响铃页，带贪睡/关闭 action。 */
    fun buildRingingNotification(context: Context, alarm: AlarmEntity): android.app.Notification {
        val alarmId = alarm.id
        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val activityPi = PendingIntent.getActivity(
            context,
            10000 + alarmId.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("闹钟")
            .setContentText(alarm.eventName.ifEmpty { "该做事了" })
            .setSubText(String.format("%02d:%02d", alarm.hour, alarm.minute))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(activityPi, true)
            .setContentIntent(activityPi)
        // 贪睡关闭（snoozeMinutes <= 0）时不显示贪睡按钮，避免点击立即再响
        if (alarm.snoozeMinutes > 0) {
            builder.addAction(0, "贪睡", actionPendingIntent(context, AlarmReceiver.ACTION_SNOOZE, alarmId))
        }
        return builder
            .addAction(0, "关闭", actionPendingIntent(context, AlarmReceiver.ACTION_DISMISS, alarmId))
            .build()
    }

    fun cancel(context: Context, alarmId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID + alarmId.toInt())
    }

    private fun actionPendingIntent(context: Context, action: String, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        // requestCode 含 alarmId：不同闹钟的关闭/贪睡互不冲突（extra 不参与 PendingIntent 相等性判断）
        return PendingIntent.getBroadcast(
            context,
            2000 + alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
