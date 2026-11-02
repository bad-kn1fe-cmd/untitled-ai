package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.RelationshipTableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationship_tables ORDER BY timestamp DESC")
    fun getAllRelationshipTablesFlow(): Flow<List<RelationshipTableEntity>>

    @Query("SELECT * FROM relationship_tables WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getRelationshipTablesForChat(chatId: Long): Flow<List<RelationshipTableEntity>>

    @Query("SELECT * FROM relationship_tables WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRelationshipTable(chatId: Long): RelationshipTableEntity?

    @Query("SELECT * FROM relationship_tables WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRelationshipTableFlow(chatId: Long): Flow<RelationshipTableEntity?>

    @Query("SELECT * FROM relationship_tables WHERE chatId = :chatId AND timestamp <= :maxTimestamp ORDER BY timestamp ASC")
    suspend fun getRelationshipTablesUpToTimestamp(chatId: Long, maxTimestamp: Long): List<RelationshipTableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationshipTable(table: RelationshipTableEntity): Long

    @Query("DELETE FROM relationship_tables WHERE chatId = :chatId")
    suspend fun deleteRelationshipTablesForChat(chatId: Long)
}
