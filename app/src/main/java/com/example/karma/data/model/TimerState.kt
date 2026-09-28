package com.example.karma.data.model

/**
 * 计时器的完整状态。持久化到 Room，跨进程存活。
 */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val startElapsed: Long = 0L,       // v4.3：System.currentTimeMillis() 墙钟时间戳 开始点
    val resumeElapsed: Long = 0L,      // v4.3：System.currentTimeMillis() 墙钟时间戳 最近恢复点
    val accumulatedMs: Long = 0L,      // 暂停时累计的毫秒数
    val selectedScore: Float = 0f,
    val selectedEvent: String = "",
) {
    /** 当前总已计时毫秒数（考虑暂停/运行状态） */
    fun currentElapsedMs(): Long {
        return when (status) {
            // v4.3：改用墙钟时间，跨设备重启连续（原 elapsedRealtime 重启归零导致时长丢失）
            TimerStatus.RUNNING -> accumulatedMs + (System.currentTimeMillis() - resumeElapsed)
            TimerStatus.PAUSED -> accumulatedMs
            TimerStatus.IDLE, TimerStatus.STOPPED -> accumulatedMs
        }
    }

    /** 格式化为 HH:MM:SS */
    fun formatTime(): String {
        val totalSeconds = currentElapsedMs() / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

enum class TimerStatus { IDLE, RUNNING, PAUSED, STOPPED }
