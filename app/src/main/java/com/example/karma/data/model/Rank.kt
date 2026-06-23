package com.example.karma.data.model

data class Rank(
    val min: Float,
    val max: Float,
    val level: Int,
    val name: String,
    val colorHex: Long,
) {
    companion object {
        fun listFrom(
            thresholds: List<Float>,
            names: List<String>,
            colors: List<Long>,
        ): List<Rank> {
            val result = mutableListOf<Rank>()
            for (i in 0 until names.size) {
                val min = if (i == 0) 0f else thresholds.getOrElse(i - 1) { 360f }
                val max = if (i == names.size - 1) Float.MAX_VALUE else thresholds.getOrElse(i) { 360f }
                result.add(
                    Rank(
                        min = min,
                        max = max,
                        level = i + 1,
                        name = names.getOrElse(i) { "?" },
                        colorHex = colors.getOrElse(i) { 0xFFFFFFFF },
                    )
                )
            }
            return result
        }
    }
}
