package com.example.karma.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.karma.data.model.Fraction

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

    // ===== 事件默认分数（与三个预设列表同下标一一对应，v3.13 新增） =====
    // 存带符号的实际分数：善业为正，恶业/善果为负（设置页输入正数，保存时按分类取负）
    val goodDeedDefaultScores: List<Float> = emptyList(),
    val badDeedDefaultScores: List<Float> = emptyList(),
    val goodResultDefaultScores: List<Float> = emptyList(),

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

    // ===== 每日必做 =====
    val dailyMustDoDeedNames: List<String> = emptyList(),
    val dailyMustDoDeedPenalties: List<Float> = emptyList(),
    val dailyMustDoLastDate: String = "",
    val dailyMustDoDoneSet: List<String> = emptyList(),
    val dailyMustDoDeeds: List<DailyMustDoDeed> = emptyList(),

    // ===== 计时持久化 =====
    val timerStatus: String = "IDLE",           // IDLE / RUNNING / PAUSED / STOPPED
    val timerStartElapsed: Long = 0L,           // v4.3：System.currentTimeMillis() 墙钟时间戳 开始点
    val timerResumeElapsed: Long = 0L,          // v4.3：System.currentTimeMillis() 墙钟时间戳 最近恢复点
    val timerAccumulatedMs: Long = 0L,          // 暂停时累计的毫秒数
    val timerSelectedScore: Float = 0f,
    val timerSelectedEvent: String = "",

    // ===== 启动经文 =====
    val splashScripture: String = "凡所有相，皆是虚妄。若见诸相非相，即见如来。",
    val splashDurationSec: Int = 3,

    // ===== 誓约（进行中） =====
    val bets: List<Bet> = emptyList(),

    // ===== 占卜每日次数限制 =====
    // 废弃字段（divinationLimitLow/High/Boundary）：列保留以兼容已升级到 v16 的旧库，
    // 逻辑改用每个阶位各自的占卜次数（正阶：rankDivinationLimits[level-1]；负阶：negativeRankDivinationLimits[-level-1]）
    // 默认：1~2阶 0 次、3~5阶 2 次、6阶及以上 3 次；负数阶位全部 0 次
    val divinationLimitLow: Int = 2,
    val divinationLimitHigh: Int = 3,
    val divinationLimitBoundary: Int = 5,
    val rankDivinationLimits: List<Int> = listOf(0, 0, 2, 2, 2, 3, 3, 3, 3),
    val divinationDate: String = "",          // 最近一次占卜日期（yyyy-MM-dd），用于按天重置
    val divinationCount: Int = 0,             // 当天已占卜次数（气运测试不计入）

    // ===== 负数阶位 =====
    // 阈值降序：(-10,0]→-1，(-20,-10]→-2，…，(-∞,-80]→-9；数量随负阶增删变化
    val negativeRankNames: List<String> = listOf(
        "微愆", "过失", "迷途", "堕落", "沉沦", "罪业", "空亡", "深渊", "无间"
    ),
    val negativeRankThresholds: List<Float> = listOf(-10f, -20f, -30f, -40f, -50f, -60f, -70f, -80f),
    // 负阶默认颜色渐变：-1 级 (80,80,80) → -9 级 (0,0,0)，每级 -10
    val negativeRankColors: List<Long> = listOf(
        0xFF505050L, 0xFF464646L, 0xFF3C3C3CL, 0xFF323232L, 0xFF282828L,
        0xFF1E1E1EL, 0xFF141414L, 0xFF0A0A0AL, 0xFF000000L,
    ),
    val negativeRankDivinationLimits: List<Int> = List(9) { 0 },    // 负阶占卜次数全部默认 0

    // ===== 乘法功能（主页面左栏乘法按钮，可增删改排序） =====
    // 用分数精确存储（1/3、1/7…），避免 Float 精度与显示问题
    val multiplierPresets: List<Fraction> = listOf(
        Fraction(1, 3), Fraction(2, 3), Fraction(1, 4), Fraction(1, 2), Fraction(3, 4),
        Fraction(3, 2), Fraction(2, 1), Fraction(5, 2), Fraction(3, 1),
    ),

    // ===== 阶位自证（v4.0） =====
    // 设置项：开关 + 特效颜色（数轴粗线默认金色）
    val proofEnabled: Boolean = false,               // 自证功能总开关
    val proofLineColor: Long = 0xFFFFD700L,          // 数轴粗线（默认金色，可自定义）
    val proofGlowColor: Long = 0xFFFFFFFFL,          // 光晕粒子
    val proofCountdownBg: Long = 0xFF8B0000L,        // 倒计时矩形背景
    val proofCountdownText: Long = 0xFFFFFFFFL,      // 倒计时文字
    val proofSuccessColor: Long = 0xFF69f0aeL,       // 成功弹窗主色
    val proofFailColor: Long = 0xFFff5252L,          // 失败弹窗主色
    // 自证进行中状态（持久化，防进程被杀丢失）
    val proofActive: Boolean = false,
    val proofStartTime: Long = 0L,                   // 开始时间戳
    val proofDurationMs: Long = 0L,                  // 时长（毫秒）
    val proofStartRankLevel: Int = 0,                // 徽章停留阶位（主动=当前阶位；被动=加分前阶位）
    val proofTargetLevel: Int = 0,                   // 成功目标阶位（主动=下一正阶位；被动=已达阶位）
    val proofGuardLevel: Int = 0,                    // 降级守卫阶位（主动=起始阶位；被动=已达阶位）
    val proofReward: Float = 0f,                     // 成功奖励分
    val proofPenalty: Float = 0f,                    // 失败惩罚分
) {
    // ===== 事件默认分数归一化（v3.13） =====

    /**
     * 将三个事件默认分数列表与对应名称列表长度对齐：
     * 缺失的按分类补默认值（善业 +1，恶业/善果 −1），多余的裁剪。
     * 防止旧数据 / 导入数据缺列导致列表不同步（同下标访问越界）。
     */
    fun withNormalizedEventScores(): KarmaSettingsEntity {
        fun align(names: List<String>, scores: List<Float>, fallback: Float): List<Float> =
            names.indices.map { i -> scores.getOrElse(i) { fallback } }
        return copy(
            goodDeedDefaultScores = align(goodDeedPresets, goodDeedDefaultScores, 1f),
            badDeedDefaultScores = align(badDeedPresets, badDeedDefaultScores, -1f),
            goodResultDefaultScores = align(goodResultPresets, goodResultDefaultScores, -1f),
        )
    }
}
