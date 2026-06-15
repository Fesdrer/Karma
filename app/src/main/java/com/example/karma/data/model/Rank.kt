package com.example.karma.data.model

data class Rank(
    val min: Float,
    val max: Float,
    val level: Int,
    val name: String,
    val colorHex: Long,
) {
    companion object {
        val RANKS = listOf(
            Rank(0f, 10f, 1, "壹阶", 0xFF0055ff),
            Rank(10f, 30f, 2, "贰阶", 0xFF0077ff),
            Rank(30f, 60f, 3, "叁阶", 0xFF0099ff),
            Rank(60f, 100f, 4, "肆阶", 0xFF00bbff),
            Rank(100f, 150f, 5, "伍阶", 0xFF00ddaa),
            Rank(150f, 210f, 6, "陆阶", 0xFF44dd44),
            Rank(210f, 280f, 7, "柒阶", 0xFFddaa00),
            Rank(280f, 360f, 8, "捌阶", 0xFFff5500),
            Rank(360f, Float.MAX_VALUE, 9, "玖阶", 0xFFff0000),
        )
    }
}
