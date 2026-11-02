package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): CharacterEntity?

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterFlow(id: Long): Flow<CharacterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacterById(id: Long)

    @Query("SELECT * FROM characters")
    suspend fun getAllCharactersList(): List<CharacterEntity>

    @Query("DELETE FROM characters WHERE name IN ('Алиса', 'Марк')")
    suspend fun deleteAliceAndMark()

    @Query("SELECT * FROM characters WHERE name = :name ORDER BY id ASC")
    suspend fun getCharactersByName(name: String): List<CharacterEntity>

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun getCharacterCount(): Int

    @Query("UPDATE characters SET isPinned = :isPinned WHERE id = :id")
    suspend fun setCharacterPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE characters SET isArchived = :isArchived WHERE id = :id")
    suspend fun setCharacterArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE characters SET isHidden = :isHidden WHERE id = :id")
    suspend fun setCharacterHidden(id: Long, isHidden: Boolean)

    @Query("UPDATE characters SET lastInteractedAt = :timestamp WHERE id = :id")
    suspend fun updateLastInteractedAt(id: Long, timestamp: Long)
}
