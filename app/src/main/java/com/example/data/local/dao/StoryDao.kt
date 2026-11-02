package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories")
    suspend fun getAllStoriesList(): List<StoryEntity>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Long): StoryEntity?

    @Query("SELECT * FROM stories WHERE id = :id")
    fun getStoryFlow(id: Long): Flow<StoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStoryById(id: Long)

    @Query("UPDATE stories SET isPinned = :isPinned WHERE id = :id")
    suspend fun setStoryPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE stories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setStoryArchived(id: Long, isArchived: Boolean)

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int
}
