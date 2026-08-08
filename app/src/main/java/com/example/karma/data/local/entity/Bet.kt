package com.example.karma.data.local.entity

/**
 * 一条进行中的誓约（内容/期限为纯文本，不做任何自动判断）
 */
data class Bet(
    val content: String,        // 对赌内容（纯文本）
    val deadline: String,       // 时间期限（纯文本）
    val successPoints: Float,   // 成功加分（正数）
    val failurePoints: Float,   // 失败减分（正数；失败时从总分扣除）
    val createdAt: Long,        // 立誓时间戳（详情展示用）
)
