package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lore_chunks")
data class LoreChunkEntity(
    @PrimaryKey val id: String,
    val characterId: Long,
    val text: String,
    val keywordsJson: String,
    val embeddingJson: String?
)
