package com.example.karma.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
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
    val axisLabelColor: Long = -1L, // 0xFFFFFFFF = 完全不透明白色
    val axisTickThickness: Float = 3f,
    val axisLabelFontSize: Float = 28f,
    val axisDisplayRange: Float = 100f,
    val showNearbyTicks: Boolean = false,
    val nearbyTickRange: Float = 10f,
    val axisQuarterValue: Float = 30f,
    val rankColors: List<Long> = listOf(
        0xFF0055ffL, 0xFF0077ffL, 0xFF0099ffL, 0xFF00bbffL,
        0xFF00ddaaL, 0xFF44dd44L, 0xFFddaa00L, 0xFFff5500L, 0xFFff0000L
    ),

    // ===== 右：历史记录 =====
    val historyLineThickness: Float = 5f,
    val historyDotRadius: Float = 8f,

    // ===== 中间光点 =====
    val guideLineWidth: Float = 6f,
    @ColumnInfo(defaultValue = "-65536")
    val dotColor: Long = 0xFFFF0000L,

    // ===== 业力衰减 =====
    val decayEnabled: Boolean = false,
    val decayHour: Int = 23,
    val decayMinute: Int = 0,
    val lastDecayDate: String = "",
    val rankDecayAmounts: List<Float> = listOf(
        2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f
    ),

    val rankThresholds: List<Float> = listOf(10f, 30f, 60f, 100f, 150f, 210f, 280f, 360f),
    val rankNames: List<String> = listOf("壹阶", "贰阶", "叁阶", "肆阶", "伍阶", "陆阶", "柒阶", "捌阶", "玖阶"),

    // ===== 背景渐变 =====
    val themeGradientBaseColor: Long = 0xFF141923,   // 顶部色 (20,25,35)
    val themeGradientAccentColor: Long = 0xFF3C2A05,  // 底部色 (60,42,5)

    // ===== 运气增幅 =====
    val luckEnabled: Boolean = false,
    val luckT: Float = 7f,
    val luckB: Float = 1f,
    val luckW: Float = 100f,
)
