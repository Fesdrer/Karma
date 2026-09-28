package com.example.karma.data.local.entity

/**
 * 一条进行中的誓约（v4.3：终止时间由纯文本改为时间戳，可判定是否到期）
 */
data class Bet(
    val content: String,        // 对赌内容（纯文本）
    val deadlineAt: Long,       // 终止时间戳（v4.3：原 deadline 纯文本改为时间）
    val successPoints: Float,   // 成功加分（正数）
    val failurePoints: Float,   // 失败减分（正数；失败时从总分扣除）
    val createdAt: Long,        // 立誓时间戳（详情展示用）
)
