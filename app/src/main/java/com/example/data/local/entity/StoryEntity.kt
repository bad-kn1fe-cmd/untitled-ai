package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val shortDescription: String = "",
    val lore: String = "",
    val worldRules: String = "",
    val characterIdsJson: String = "[]", // JSON array of Long (character IDs)
    val coverUri: String = "",
    val tags: String = "[]", // JSON array of strings
    val customSystemPrompt: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
