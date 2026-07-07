package com.example.karma.data.local.entity

/**
 * 每日必做事件。
 * @param name 事件名称（对应善业预设列表中的事件名）
 * @param penalty 未完成扣分值（正数，扣分时从总分减去）
 * @param vis 是否已完成：0=未做，1=已做
 */
data class DailyMustDoDeed(
    val name: String,
    val penalty: Float,
    val vis: Int = 0,
)
