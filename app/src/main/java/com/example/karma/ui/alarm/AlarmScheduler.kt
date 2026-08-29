package com.example.karma.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.karma.MainActivity
import com.example.karma.data.local.entity.AlarmEntity
import java.util.Calendar

/**
 * 闹钟调度：
 * - Android 12+ 精确闹钟需要 SCHEDULE_EXACT_ALARM（特殊权限）：有权限 → setAlarmClock
 *   （精确、Doze 不延迟、状态栏显示"下一个闹钟"）；无权限 → 降级为普通 set（可能被 Doze 延迟，但不崩溃）
 * - 同一闹钟的 PendingIntent requestCode = alarmId，更新/取消精确对应
 */
object AlarmScheduler {

    /** 注册（或更新）闹钟触发；一次性闹钟时间已过 → 仅取消不调度。 */
    fun schedule(context: Context, alarm: AlarmEntity) {
        val triggerAt = computeNextTrigger(alarm, System.currentTimeMillis())
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = ringPendingIntent(context, alarm.id)
        if (triggerAt == null) {
            am.cancel(pi)
            return
        }
        // 优先精确闹钟；无 SCHEDULE_EXACT_ALARM 权限或调用被拒时降级为普通 set，绝不崩溃
        if (canScheduleExact(am)) {
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent(context, alarm.id)), pi)
                return
            } catch (_: SecurityException) {
                // 权限被拒/撤销：走降级路径
            }
        }
        try {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: Exception) {
            // 极端情况（被系统限制）也不崩溃
        }
    }

    /** 贪睡：now + alarm.snoozeMinutes 触发（requestCode 相同 → 覆盖原触发）。 */
    fun scheduleSnooze(context: Context, alarm: AlarmEntity) {
        val triggerAt = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = ringPendingIntent(context, alarm.id)
        if (canScheduleExact(am)) {
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent(context, alarm.id)), pi)
                return
            } catch (_: SecurityException) {
                // 权限被拒/撤销：走降级路径
            }
        }
        try {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: Exception) {
            // 极端情况也不崩溃
        }
    }

    /** Android 12+ 是否有精确闹钟权限（低版本恒为 true）。 */
    private fun canScheduleExact(am: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /** 取消闹钟触发（关闭开关/删除/一次性过期时）。 */
    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(ringPendingIntent(context, alarmId))
    }

    private fun ringPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RING
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * 计算下次触发时间（毫秒）。
     * - 仅一次（repeatDays 空）：当天该时间；已过 → null（不再触发）
     * - 重复：未来最近一个匹配星期几的该时间
     */
    fun computeNextTrigger(alarm: AlarmEntity, now: Long): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (alarm.repeatDays.isEmpty()) {
            cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
            cal.set(Calendar.MINUTE, alarm.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return if (cal.timeInMillis > now) cal.timeInMillis else null
        }
        val days = alarm.repeatDays
        // 从今天开始最多查 8 天（覆盖一周 + 今天兜底），必然命中非空集合
        repeat(8) {
            if (isoDayOfWeek(cal) in days) {
                cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
                cal.set(Calendar.MINUTE, alarm.minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis > now) return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    /** Calendar.DAY_OF_WEEK（SUNDAY=1..SATURDAY=7）→ ISO 星期（1=周一..7=周日）。 */
    fun isoDayOfWeek(cal: Calendar): Int {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    /** 周几集合 → 显示文本：空=仅一次；7 天=每天；否则列出周几。 */
    fun repeatText(repeatDays: List<Int>): String {
        if (repeatDays.isEmpty()) return "仅一次"
        if (repeatDays.size == 7) return "每天"
        val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return repeatDays.sorted().joinToString("、") { names.getOrElse(it - 1) { "?" } }
    }
}
