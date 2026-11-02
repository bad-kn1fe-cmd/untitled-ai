package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE characterId = :characterId ORDER BY updatedAt DESC")
    fun getChatsForCharacter(characterId: Long): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatFlow(id: Long): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: Long)

    @Query("UPDATE chats SET isTextingMode = :isTexting, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setTextingMode(chatId: Long, isTexting: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET isNsfwEnabled = :nsfw, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setNsfwEnabled(chatId: Long, nsfw: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setChatPinned(chatId: Long, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setChatArchived(chatId: Long, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET title = :title, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: Long, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET draftText = :draft WHERE id = :chatId")
    suspend fun updateChatDraft(chatId: Long, draft: String)

    @Query("UPDATE chats SET notes = :notes, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatNotes(chatId: Long, notes: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET personaId = :personaId, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setChatPersona(chatId: Long, personaId: Long?, timestamp: Long = System.currentTimeMillis())
}
