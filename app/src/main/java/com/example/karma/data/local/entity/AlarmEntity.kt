package com.example.karma.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 闹钟（照手机闹钟设计）：
 * - hour/minute：触发时间
 * - repeatDays：周一(1)~周日(7) 的 ISO 星期集合；空列表 = 仅一次
 * - eventName：闹钟名称（从善业/恶业/善果预设或自定义文本选择）
 * - ringtoneUri：铃声 URI；空 = 系统默认闹钟铃声
 * - vibrate：响铃时是否震动
 * - snoozeMinutes：贪睡时长（分钟）
 * - enabled：开关（关 = 保留但不再触发）
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val enabled: Boolean = true,
    val hour: Int = 7,
    val minute: Int = 30,
    val repeatDays: List<Int> = emptyList(),
    val eventName: String = "",
    val ringtoneUri: String = "",
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
)
