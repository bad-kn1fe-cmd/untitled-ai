package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getSummariesForChat(chatId: Long): Flow<List<SummaryEntity>>

    @Query("SELECT * FROM summaries WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSummary(chatId: Long): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE chatId = :chatId AND timestamp <= :maxTimestamp ORDER BY timestamp ASC")
    suspend fun getSummariesUpToTimestamp(chatId: Long, maxTimestamp: Long): List<SummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity): Long

    @Query("DELETE FROM summaries WHERE chatId = :chatId")
    suspend fun deleteSummariesForChat(chatId: Long)
}
