package com.example.karma.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "karma_settings")
data class KarmaSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalScore: Float = 0f,
    val scorePresets: List<Float> = listOf(-2f, -1.5f, -1f, -0.5f, 0.5f, 1f, 1.5f, 2f),
    val goodDeedPresets: List<String> = listOf(
        "帮助他人", "早起早睡", "锻炼身体", "日行一善", "孝敬父母"
    ),
    val badDeedPresets: List<String> = listOf(
        "发脾气", "浪费粮食", "口出恶言", "懒惰拖延", "伤害他人"
    ),
    val goodResultPresets: List<String> = emptyList(),

    // ===== 左：分数区域 =====
    val scoreAxisFontSize: Float = 18f,
    val scoreAxisRangeMin: Float = -6f,
    val scoreAxisRangeMax: Float = 6f,

    // ===== 中：刻度区域 =====
    val axisLabelColor: Long = 0x80FFFFFF.toLong(), // = -2130706433L
    val axisTickThickness: Float = 6f,
    val axisLabelFontSize: Float = 28f,
    val axisDisplayRange: Float = 100f,
    val showNearbyTicks: Boolean = true,
    val nearbyTickRange: Float = 10f,
    val axisQuarterValue: Float = 15f,
    val rankColors: List<Long> = listOf(
        0xFF0055ffL, 0xFF0077ffL, 0xFF0099ffL, 0xFF00bbffL,
        0xFF00ddaaL, 0xFF44dd44L, 0xFFddaa00L, 0xFFff5500L, 0xFFff0000L
    ),

    // ===== 右：历史记录 =====
    val historyLineThickness: Float = 5f,
    val historyDotRadius: Float = 8f,

    // ===== 业力衰减 =====
    val decayEnabled: Boolean = false,
    val decayHour: Int = 23,
    val decayMinute: Int = 0,
    val lastDecayDate: String = "",
    val rankDecayAmounts: List<Float> = listOf(
        2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f
    ),
)
