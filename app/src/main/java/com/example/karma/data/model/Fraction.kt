package com.example.karma.data.model

import kotlin.math.abs
import kotlin.math.round

/**
 * 有理数（分数）：乘法按钮等场景的精确表示，避免 Float 的精度与显示问题。
 * 分子分母恒为约分后的整数；分母恒为正。
 *
 * 显示规则（format）：
 * - 分母 1 → 整数（2）
 * - 假分数且为半整数（3/2、5/2）→ 小数（1.5、2.5）
 * - 其余 → 分数（1/3、1/7、2/3）
 */
data class Fraction(
    val numerator: Int,
    val denominator: Int,
) {
    /** 十进制值（用于乘法等计算）。 */
    val value: Float get() = numerator.toFloat() / denominator

    /** 显示文本：整数→"2"，半整数假分数→"1.5"，其余→"1/7"。 */
    fun format(): String {
        val n = numerator
        val d = denominator
        if (d == 1) return n.toString()
        if (n > d && (2L * n) % d == 0L) {
            return "${n / d}.5"
        }
        return "$n/$d"
    }

    companion object {
        val ONE = Fraction(1, 1)

        /** 解析 "1/7"、"1.5"、"2" 等文本；无法解析返回 null。 */
        fun parse(text: String): Fraction? {
            val t = text.trim()
            if (t.isEmpty()) return null
            val slash = t.indexOf('/')
            if (slash > 0) {
                val n = t.substring(0, slash).toLongOrNull() ?: return null
                val d = t.substring(slash + 1).toLongOrNull() ?: return null
                if (d == 0L) return null
                return of(n, d)
            }
            // 小数或整数 → 精确转分数（"1.5" → 3/2，"2" → 2/1，"0.33" → 33/100）
            val neg = t.startsWith("-")
            val body = if (neg) t.substring(1) else t
            val dot = body.indexOf('.')
            if (dot >= 0 && body.indexOf('.', dot + 1) >= 0) return null
            val intPart = if (dot >= 0) body.substring(0, dot) else body
            val fracPart = if (dot >= 0) body.substring(dot + 1) else ""
            val intVal = intPart.toLongOrNull() ?: return null
            val fracVal = if (fracPart.isEmpty()) 0L else fracPart.toLongOrNull() ?: return null
            if (intVal < 0 || fracVal < 0) return null
            var scale = 1L
            for (i in fracPart.indices) scale *= 10L
            val num = intVal * scale + fracVal
            if (num == 0L) return Fraction(0, 1)
            return of((if (neg) -1 else 1) * num, scale)
        }

        /** 构造并约分；分母为 0 或分子分母超出 Int 时返回 null。 */
        fun of(numerator: Long, denominator: Long): Fraction? {
            if (denominator == 0L) return null
            val neg = (numerator < 0) xor (denominator < 0)
            val n = abs(numerator)
            val d = abs(denominator)
            val g = gcd(n, d).coerceAtLeast(1L)
            val nn = n / g
            val dd = d / g
            if (nn > Int.MAX_VALUE || dd > Int.MAX_VALUE) return null
            return Fraction(if (neg) -nn.toInt() else nn.toInt(), dd.toInt())
        }

        private fun gcd(a: Long, b: Long): Long {
            var x = abs(a)
            var y = abs(b)
            while (y != 0L) {
                val t = x % y
                x = y
                y = t
            }
            return x
        }

        /**
         * 把 Float 还原为最接近的有理数（枚举分母 1..1000，容差 1e-4）。
         * 仅用于兼容旧版数据库里以 Float 数组存储的乘数（0.33333334 → 1/3）。
         */
        fun fromFloat(value: Float): Fraction {
            if (value == 0f) return Fraction(0, 1)
            val v = abs(value.toDouble())
            for (d in 1..1000) {
                val n = round(v * d).toLong()
                if (abs(n.toDouble() / d - v) < 1e-4) {
                    return of(if (value < 0) -n else n, d.toLong())
                        ?: Fraction(if (value < 0) -1 else 1, 1)
                }
            }
            val scale = 100000L
            val n = round(v * scale).toLong()
            return of(if (value < 0) -n else n, scale)
                ?: Fraction(if (value < 0) -1 else 1, 1)
        }
    }
}
