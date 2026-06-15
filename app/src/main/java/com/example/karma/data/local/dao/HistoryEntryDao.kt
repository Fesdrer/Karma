package com.example.karma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.karma.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryEntryDao {

    @Query("SELECT * FROM history_entries ORDER BY timestamp ASC")
    fun getAllEntries(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_entries ORDER BY timestamp ASC")
    suspend fun getAllEntriesList(): List<HistoryEntryEntity>

    @Insert
    suspend fun insertEntry(entry: HistoryEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<HistoryEntryEntity>)

    @Query("DELETE FROM history_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun getCount(): Int

    @Query("DELETE FROM history_entries WHERE id IN (SELECT id FROM history_entries ORDER BY timestamp ASC LIMIT :excess)")
    suspend fun trimOldest(excess: Int)

    @Query("SELECT * FROM history_entries ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestEntry(): HistoryEntryEntity?
}
