package com.example.karma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.karma.data.local.entity.DailyMustDoDeed
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

    /** 只更新 lastDecayDate 单列（applyDecay 空日期初始化用，不做整行写回）。 */
    @Query("UPDATE karma_settings SET lastDecayDate = :lastDate WHERE id = 1")
    suspend fun updateLastDecayDate(lastDate: String)

    /**
     * applyDecay 落库：一次 SQL 原子更新 总分 + lastDecayDate + 每日必做 deeds，
     * 避免整行 upsert 用旧快照覆盖自证/计时等并发写入的字段（v4.2-改4）。
     */
    @Query("UPDATE karma_settings SET totalScore = :score, lastDecayDate = :lastDate, dailyMustDoDeeds = :deeds WHERE id = 1")
    suspend fun updateDecayResult(score: Float, lastDate: String, deeds: List<DailyMustDoDeed>)

    @Query("UPDATE karma_settings SET dailyMustDoDeeds = :deeds, dailyMustDoLastDate = :lastDate WHERE id = 1")
    suspend fun updateDailyMustDoFields(deeds: List<DailyMustDoDeed>, lastDate: String)

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
