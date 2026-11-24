package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LoreChunkEntity

@Dao
interface LoreChunkDao {
    @Query("SELECT * FROM lore_chunks WHERE characterId = :characterId")
    suspend fun getChunksForCharacter(characterId: Long): List<LoreChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<LoreChunkEntity>)

    @Query("DELETE FROM lore_chunks WHERE characterId = :characterId")
    suspend fun deleteChunksForCharacter(characterId: Long)
}
