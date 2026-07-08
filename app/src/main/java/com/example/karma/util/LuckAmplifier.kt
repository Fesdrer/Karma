package com.example.karma.util

import com.example.karma.data.local.entity.HistoryEntryEntity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 运气增幅值计算器。
 *
 * 公式：运气 = [K + c ∫₀ᵀ K'(τ)·e^{-aτ²} dτ] / b
 *
 * 其中：
 * - K   = 总业力 (totalScore)
 * - K'  = dK/dτ（前向时间导数），分段恒定
 * - a   = 9/(2T²)
 * - c   = W / (10 · ∫₀¹ e^{-aτ²} dτ)
 * - b   = 普通好事分值
 *
 * K 在记录间线性变化（折线），K' = dK/dτ 分段恒定。
 * 积分只算到 T 天前，再往前 e^{-aτ²} 近似为 0，可忽略。
 *
 * 实现方式：将记录按距今天数 t = τ_now - τ 转换为点列 (t_i, K_i)，
 * 对每对相邻点 (t_i < t_{i+1}) 计算前向导数 K' = (K_i - K_{i+1})/(t_{i+1} - t_i)，
 * 然后累加 K' · ∫_{t_i}^{min(t_{i+1}, T)} e^{-at²} dt（在 T 处截断）。
 *
 * 若一段的任一端点为"善果：…"或"祈福：…"（消耗业力行为），跳过该段的积分贡献。
 * 只跳较新端点不够——若较旧端点是消费事件，其 K 值被人为压低，
 * 导致 K' = (K_i - K_j)/dt 虚高，消耗业力反而增加运气。
 */
object LuckAmplifier {

    /** 一天对应的毫秒数 */
    private const val MS_PER_DAY: Double = 24.0 * 60.0 * 60.0 * 1000.0

    // ============================================================
    // erf(x) — Abramowitz & Stegun 7.1.26 近似
    // 最大误差 < 1.5×10⁻⁷
    // ============================================================

    private const val ERF_P  = 0.3275911
    private const val ERF_A1 =  0.254829592
    private const val ERF_A2 = -0.284496736
    private const val ERF_A3 =  1.421413741
    private const val ERF_A4 = -1.453152027
    private const val ERF_A5 =  1.061405429

    /**
     * 误差函数 erf(x) = 2/√π ∫₀ˣ e^{-u²} du
     */
    fun erf(x: Double): Double {
        val sign = if (x >= 0) 1.0 else -1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + ERF_P * ax)
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val t5 = t4 * t
        val poly = ERF_A1 * t + ERF_A2 * t2 + ERF_A3 * t3 + ERF_A4 * t4 + ERF_A5 * t5
        return sign * (1.0 - poly * exp(-ax * ax))
    }

    // ============================================================
    // ∫_{from}^{to} e^{-a·t²} dt
    // ============================================================

    /**
     * 计算定积分 ∫_{from}^{to} e^{-a·t²} dt
     *
     * 解析解：√π/(2√a) · [erf(√a·to) - erf(√a·from)]
     */
    fun integralExpMinusAt2(a: Double, from: Double, to: Double): Double {
        if (from >= to) return 0.0
        if (a <= 0.0) return to - from  // 退化为矩形面积

        val sqrtA = sqrt(a)
        val coeff = sqrt(Math.PI) / (2.0 * sqrtA)
        return coeff * (erf(sqrtA * to) - erf(sqrtA * from))
    }

    // ============================================================
    // 运气增幅值计算
    // ============================================================

    /**
     * 计算运气增幅值。
     *
     * @param totalScore     当前总业力 K
     * @param historyEntries 全部历史记录（按 timestamp ASC 排序）
     * @param T              T 天（参数：多少天后行为只通过总和影响）
     * @param b              b 分（参数：普通好事分值）
     * @param W              W 分（参数：最近一天降10分对应总分多少）
     * @return 运气增幅值，保留两位小数
     */
    fun computeLuckAmplification(
        totalScore: Float,
        historyEntries: List<HistoryEntryEntity>,
        T: Float,
        b: Float,
        W: Float,
    ): Float {
        // b ≤ 0 防护
        if (b <= 0f) return roundTo2(totalScore.toDouble())
        // 无记录或仅一条记录：无 K' 可算，integral = 0
        if (historyEntries.size < 2) {
            return roundTo2(totalScore.toDouble() / b.toDouble())
        }

        // T ≤ 0 防护：无有效时间窗口，积分置零
        if (T <= 0f) {
            return roundTo2(totalScore.toDouble() / b.toDouble())
        }

        // 1. 计算 a、c
        val a = 9.0 / (2.0 * T * T)
        // a 溢出防护（T 极端小导致 a → ∞）
        if (!a.isFinite()) {
            return roundTo2(totalScore.toDouble() / b.toDouble())
        }
        val cDenom = integralExpMinusAt2(a, 0.0, 1.0)   // ∫₀¹ e^{-at²} dt
        val c = W.toDouble() / (10.0 * cDenom)

        // 2. 以最后一条记录时间为"现在"（t=0），计算每条记录距现在的天数
        val lastTimestamp = historyEntries.last().timestamp
        data class Point(val tDays: Double, val karma: Float, val event: String = "")

        val points = historyEntries.map { entry ->
            val tDays = (lastTimestamp - entry.timestamp) / MS_PER_DAY
            Point(tDays, entry.totalAfter, entry.event)
        }

        // 3. 按 t 升序排列（稳定排序保留同 t 点的原始 ASC 顺序）
        val sorted = points.sortedBy { it.tDays }

        if (sorted.size < 2) {
            return roundTo2(totalScore.toDouble() / b.toDouble())
        }

        // 4. 逐段积分（在 T 处截断：段跨越 T 边界时积分只到 T）
        var totalIntegral = 0.0
        val Td = T.toDouble()
        for (i in 0 until sorted.size - 1) {
            val pI = sorted[i]      // 较新（t 较小）
            val pJ = sorted[i + 1]  // 较旧（t 较大）

            // 段已完全超出 T 范围，后续段也都超出，停止
            if (pI.tDays >= Td) break

            // 善果、祈福、业力衰减是消耗业力的行为，段任一端点为此类事件则跳过
            if (pI.event.startsWith("善果：") || pI.event.startsWith("祈福：") || pI.event == "业力衰减") continue

            val dt = pJ.tDays - pI.tDays    // 前向时间差（天）

            if (dt > 0.0) {
                // 正常情况：使用 K' * ∫f(t)dt
                val dK = pI.karma - pJ.karma          // 前向 ΔK（K_new - K_old）
                val kPrime = dK.toDouble() / dt        // K' = dK/dτ（前向时间导数）

                // 在 T 处截断：若段跨越 T 边界，积分上限截断到 T
                val segEnd = if (pJ.tDays > Td) Td else pJ.tDays
                val segIntegral = integralExpMinusAt2(a, pI.tDays, segEnd)
                totalIntegral += kPrime * segIntegral
            } else if (dt == 0.0) {
                // 时间相同：使用 y*f(t) 避免除以 0
                // pJ 在 pI 之后（稳定排序保持原始 ASC 顺序），故 pJ 较新
                val y = (pJ.karma - pI.karma).toDouble() // 前向业力变化量
                val f = exp(-a * pI.tDays * pI.tDays)   // f(t) = e^{-at²}
                totalIntegral += y * f
            }
            // dt < 0：理论不应发生，静默跳过
        }

        // 5. 运气 = (K + c·integral) / b
        val luck = (totalScore.toDouble() + c * totalIntegral) / b.toDouble()
        return roundTo2(luck)
    }

    // ============================================================
    // 辅助
    // ============================================================

    private fun roundTo2(value: Double): Float {
        return (kotlin.math.round(value * 100.0) / 100.0).toFloat()
    }
}
