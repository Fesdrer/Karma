package com.example.karma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.karma.data.local.entity.KarmaSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KarmaSettingsDao {

    @Query("SELECT * FROM karma_settings WHERE id = 1")
    fun getSettings(): Flow<KarmaSettingsEntity?>

    @Query("SELECT * FROM karma_settings WHERE id = 1")
    suspend fun getSettingsOnce(): KarmaSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: KarmaSettingsEntity)

    @Query("UPDATE karma_settings SET totalScore = :score WHERE id = 1")
    suspend fun updateTotalScore(score: Float)

    @Query("UPDATE karma_settings SET scorePresets = :presets WHERE id = 1")
    suspend fun updateScorePresets(presets: List<Float>)

    @Query("UPDATE karma_settings SET goodDeedPresets = :presets WHERE id = 1")
    suspend fun updateGoodDeedPresets(presets: List<String>)

    @Query("UPDATE karma_settings SET badDeedPresets = :presets WHERE id = 1")
    suspend fun updateBadDeedPresets(presets: List<String>)

    @Query("UPDATE karma_settings SET goodResultPresets = :presets WHERE id = 1")
    suspend fun updateGoodResultPresets(presets: List<String>)

    @Query("""
        UPDATE karma_settings SET
            timerStatus = :timerStatus,
            timerStartElapsed = :timerStartElapsed,
            timerResumeElapsed = :timerResumeElapsed,
            timerAccumulatedMs = :timerAccumulatedMs,
            timerSelectedScore = :timerSelectedScore,
            timerSelectedEvent = :timerSelectedEvent
        WHERE id = 1
    """)
    suspend fun updateTimerFields(
        timerStatus: String,
        timerStartElapsed: Long,
        timerResumeElapsed: Long,
        timerAccumulatedMs: Long,
        timerSelectedScore: Float,
        timerSelectedEvent: String,
    )
}
