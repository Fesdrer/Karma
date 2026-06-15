package com.example.karma.data.model

data class HistoryEntry(
    val id: Long = 0,
    val timestamp: Long,
    val delta: Float,
    val event: String,
    val type: String,
    val totalAfter: Float,
)
