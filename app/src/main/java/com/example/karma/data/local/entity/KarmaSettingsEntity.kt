package com.example.karma.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "karma_settings")
data class KarmaSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalScore: Float = 0f,
    val scorePresets: List<Float> = listOf(-2f, -1.5f, -1f, -0.5f, 0.5f, 1f, 1.5f, 2f),
    val eventPresets: List<String> = listOf(
        "帮助他人", "早起早睡", "锻炼身体", "发脾气", "浪费粮食", "口出恶言"
    ),
)
