package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId")]
)
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val title: String,
    val relationshipType: String = "strangers", // strangers, acquaintances, friends, close_friends, romantic, custom
    val customRelationship: String = "",
    val formalityDistance: String = "informal", // formal, informal
    val isNsfwEnabled: Boolean = false,
    val isTextingMode: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val providerProfileId: Long? = null,
    val personaId: Long? = null,
    val draftText: String = "",
    val notes: String = "",
    val summaryFrequency: Int = 18, // message count threshold (15-20)
    val roleplayMode: String = "medium", // light, medium, hard, cinematic, psychological
    val responseLength: String = "medium", // short, medium, long, epic
    val atmosphere: String = "default", // default (from settings), cozy, noir, fantasy, cyberpunk, romance, thriller, mystic_forest, space, none
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
