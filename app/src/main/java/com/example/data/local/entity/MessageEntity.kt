package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val sender: String, // "user", "character", "system"
    val content: String,
    val imageUrl: String? = null,
    val imagePrompt: String? = null,
    val internalReasoning: String? = null,
    val isChunked: Boolean = false,
    val isTextingMode: Boolean = false,
    val variantsJson: String? = null, // JSON array of generated reply alternatives: ["alt1", "alt2", ...]
    val currentVariantIndex: Int = 0,
    val audioFilePath: String? = null,
    val audioDurationSec: Int = 0,
    val reaction: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
