package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AiProviderProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_provider_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<AiProviderProfileEntity>>

    @Query("SELECT * FROM ai_provider_profiles ORDER BY id ASC")
    suspend fun getAllProfilesList(): List<AiProviderProfileEntity>

    @Query("SELECT * FROM ai_provider_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): AiProviderProfileEntity?

    @Query("SELECT * FROM ai_provider_profiles WHERE isDefaultText = 1 LIMIT 1")
    suspend fun getDefaultTextProfile(): AiProviderProfileEntity?

    @Query("SELECT * FROM ai_provider_profiles WHERE isDefaultText = 1 LIMIT 1")
    fun getDefaultTextProfileFlow(): Flow<AiProviderProfileEntity?>

    @Query("SELECT * FROM ai_provider_profiles WHERE isDefaultImage = 1 LIMIT 1")
    suspend fun getDefaultImageProfile(): AiProviderProfileEntity?

    @Query("SELECT * FROM ai_provider_profiles WHERE isDefaultImage = 1 LIMIT 1")
    fun getDefaultImageProfileFlow(): Flow<AiProviderProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AiProviderProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<AiProviderProfileEntity>)

    @Update
    suspend fun updateProfile(profile: AiProviderProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: AiProviderProfileEntity)

    @Query("UPDATE ai_provider_profiles SET isDefaultText = 0")
    suspend fun clearDefaultText()

    @Query("UPDATE ai_provider_profiles SET isDefaultImage = 0")
    suspend fun clearDefaultImage()

    @Transaction
    suspend fun setDefaultTextProfile(id: Long) {
        clearDefaultText()
        setProfileDefaultText(id)
    }

    @Transaction
    suspend fun setDefaultImageProfile(id: Long) {
        clearDefaultImage()
        setProfileDefaultImage(id)
    }

    @Query("UPDATE ai_provider_profiles SET isDefaultText = 1 WHERE id = :id")
    suspend fun setProfileDefaultText(id: Long)

    @Query("UPDATE ai_provider_profiles SET isDefaultImage = 1 WHERE id = :id")
    suspend fun setProfileDefaultImage(id: Long)

    @Query("SELECT COUNT(*) FROM ai_provider_profiles")
    suspend fun getProfileCount(): Int
}
