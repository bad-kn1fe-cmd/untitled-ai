package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val chatId: Long,
    val characterId: Long = 0,
    val chapterName: String = "",
    val style: String = "anime_color", // "anime_color", "manga_bw", "marvel_dc", "cyberpunk", "webtoon", "watercolor"
    val panelsJson: String = "[]",
    val coverUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
