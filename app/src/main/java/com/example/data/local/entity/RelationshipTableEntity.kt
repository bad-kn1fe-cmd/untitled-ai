package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relationship_tables",
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
data class RelationshipTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val status: String = "Незнакомцы",
    val trustScore: Int = 10, // 0 - 100
    val mood: String = "Нейтральное",
    val keyEvents: String = "[]", // JSON array of strings
    val openPlotThreads: String = "[]", // JSON array of strings
    val boardJson: String = "", // JSON snapshot of the multi-character Relationship Board
    val timestamp: Long = System.currentTimeMillis()
)
