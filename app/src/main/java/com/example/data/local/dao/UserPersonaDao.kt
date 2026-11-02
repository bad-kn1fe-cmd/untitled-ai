package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPersonaDao {
    @Query("SELECT * FROM user_personas ORDER BY isSelected DESC, id ASC")
    fun getAllPersonas(): Flow<List<UserPersonaEntity>>

    @Query("SELECT * FROM user_personas")
    suspend fun getAllPersonasList(): List<UserPersonaEntity>

    @Query("SELECT * FROM user_personas WHERE id = :id")
    suspend fun getPersonaById(id: Long): UserPersonaEntity?

    @Query("SELECT * FROM user_personas WHERE isSelected = 1 LIMIT 1")
    fun getActivePersonaFlow(): Flow<UserPersonaEntity?>

    @Query("SELECT * FROM user_personas WHERE isSelected = 1 LIMIT 1")
    suspend fun getActivePersona(): UserPersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: UserPersonaEntity): Long

    @Update
    suspend fun updatePersona(persona: UserPersonaEntity)

    @Delete
    suspend fun deletePersona(persona: UserPersonaEntity)

    @Query("UPDATE user_personas SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE user_personas SET isSelected = 1 WHERE id = :id")
    suspend fun selectPersona(id: Long)

    @Query("SELECT COUNT(*) FROM user_personas")
    suspend fun getPersonaCount(): Int
}
