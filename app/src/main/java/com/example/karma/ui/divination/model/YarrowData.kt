package com.example.karma.ui.divination.model

/** 单次"变"的结果 */
data class OneChange(
    val leftCount: Int,
    val rightCount: Int,
    val hang: Int,            // =1
    val leftRem: Int,         // 左余 1-4
    val rightRem: Int,        // 右余 1-4
    val collected: Int,       // 归奇 = hang + leftRem + rightRem
    val remaining: Int,       // 剩余策数
)

/** 一爻：三变后的结果 */
data class HexagramLine(
    val value: Int,           // 6/7/8/9
    val isYang: Boolean,      // 值是否为奇数（7/9=阳）
    val isChanging: Boolean,  // 6或9为变爻
) {
    val label: String get() = when (value) {
        6 -> "老阴 ×"
        7 -> "少阳 —"
        8 -> "少阴 - -"
        9 -> "老阳 ○"
        else -> "?"
    }
}

/** 最终结果 */
data class YarrowResult(
    val lines: List<HexagramLine>,  // 从初爻到上爻（6个）
)

/** 一根策（蓝色线段） */
class Stick(
    val id: Int,
    var x: Float,
    var y: Float,
    var width: Float,
    var targetX: Float,
    var targetY: Float,
    var targetWidth: Float,
    var visible: Boolean = true,
)

/** 策的组别 */
enum class StickGroup { TAIJI, WORK, HANGED, COLLECTED }
