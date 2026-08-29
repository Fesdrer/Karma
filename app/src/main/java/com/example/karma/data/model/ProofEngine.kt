package com.example.karma.data.model

import com.example.karma.data.local.entity.KarmaSettingsEntity
import java.util.Calendar

/**
 * 阶位自证核心逻辑（纯函数，无 UI 依赖）。
 *
 * 主动自证：起始阶位=当前，目标=下一正阶位，降级守卫=起始阶位。
 *   成功=时长内达到目标（或更高）立即结束；失败=超时未达 或 中途降到守卫以下立即失败。
 * 被动自证：加分跨入正阶位时触发，起始阶位(徽章)=加分前阶位，目标=已达阶位，守卫=已达阶位。
 *   成功=保持(>=目标)到时长结束；失败=中途降到守卫以下立即失败。
 */
object ProofEngine {

    /** 下一正阶位 level：当前负阶(<1)→1；否则当前+1。 */
    fun nextPositiveRankLevel(currentLevel: Int): Int = if (currentLevel < 1) 1 else currentLevel + 1

    /** 分数所在阶位 level（正阶 1..N；负阶 -1..-9，与 repository.getRank 一致）。 */
    fun rankLevelOf(score: Float, settings: KarmaSettingsEntity): Int {
        if (score < 0f) {
            val ts = settings.negativeRankThresholds
            val names = settings.negativeRankNames
            var level = names.size.coerceAtLeast(1)
            for (i in ts.indices) {
                if (score > ts[i]) {
                    level = i + 1
                    break
                }
            }
            return -level
        }
        val thresholds = settings.rankThresholds
        var level = 1
        for (t in thresholds) {
            if (score >= t) level++ else break
        }
        return level
    }

    /** 某正阶位 level 的下限分数（1 阶=0，N 阶=rankThresholds[N-2]）。 */
    fun thresholdOf(level: Int, settings: KarmaSettingsEntity): Float {
        if (level <= 1) return 0f
        return settings.rankThresholds.getOrElse(level - 2) { settings.rankThresholds.lastOrNull() ?: 360f }
    }

    /** 距离下一正阶位的分数差（主动按钮可用判定 <10 分）。 */
    fun distanceToNextPositiveRank(score: Float, currentLevel: Int, settings: KarmaSettingsEntity): Float {
        val target = nextPositiveRankLevel(currentLevel)
        return thresholdOf(target, settings) - score
    }

    /**
     * 自证时长的最短合法值：结束时刻必须覆盖下一个业力衰减时刻（衰减开启时）。
     * @return 最短时长（毫秒）；衰减关闭或无需限制返回 0（无下限）。
     */
    fun minDurationToCoverDecay(settings: KarmaSettingsEntity, startTime: Long): Long {
        if (!settings.decayEnabled) return 0L
        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        val next = cal.clone() as Calendar
        next.set(Calendar.HOUR_OF_DAY, settings.decayHour)
        next.set(Calendar.MINUTE, settings.decayMinute)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        if (next.timeInMillis <= startTime) {
            next.add(Calendar.DAY_OF_MONTH, 1)
        }
        return (next.timeInMillis - startTime).coerceAtLeast(0L)
    }

    /** 判定成功：分数所在阶位 >= 目标阶位。 */
    fun isSuccess(score: Float, settings: KarmaSettingsEntity, targetLevel: Int): Boolean =
        rankLevelOf(score, settings) >= targetLevel

    /** 判定降级（中途立即失败）：分数所在阶位 < 守卫阶位。 */
    fun isDowngraded(score: Float, settings: KarmaSettingsEntity, guardLevel: Int): Boolean =
        rankLevelOf(score, settings) < guardLevel
}
