package com.example.karma.ui.alarm

import android.content.Context
import com.example.karma.KarmaApplication
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.runBlocking

/**
 * 闹钟响铃后的处理（通知按钮 / 响铃页共用）：
 * - 关闭：停铃；一次性 → 写 DB 关闭开关（保留条目）；重复 → 重新调度下次触发
 * - 贪睡：停铃 + 立即调度 now+贪睡时长（覆盖原触发）
 */
object AlarmActions {

    private fun repository(context: Context): KarmaRepository? {
        return (context.applicationContext as? KarmaApplication)?.container?.repository
    }

    fun dismiss(context: Context, alarmId: Long) {
        AlarmRingingService.stopRinging(context)
        AlarmNotifications.cancel(context, alarmId)
        val repo = repository(context) ?: return
        runBlocking {
            val alarm = repo.getAlarmById(alarmId) ?: return@runBlocking
            if (alarm.repeatDays.isEmpty()) {
                // 一次性闹钟：响过即关（保留条目，开关自动关闭），不再调度
                repo.upsertAlarm(alarm.copy(enabled = false))
                AlarmScheduler.cancel(context, alarmId)
            } else {
                // 重复闹钟：重新调度真正的下次触发
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }

    fun snooze(context: Context, alarmId: Long) {
        AlarmRingingService.stopRinging(context)
        AlarmNotifications.cancel(context, alarmId)
        val repo = repository(context) ?: return
        runBlocking {
            val alarm = repo.getAlarmById(alarmId) ?: return@runBlocking
            AlarmScheduler.scheduleSnooze(context, alarm)
        }
    }
}
