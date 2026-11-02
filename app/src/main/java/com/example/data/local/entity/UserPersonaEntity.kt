package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_personas")
data class UserPersonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUri: String = "drawable://avatar_app_dark",
    val shortDescription: String = "",
    val personality: String = "",
    val appearance: String = "",
    val habits: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val age: Int = 20,
    val gender: String = "", // "Мужской", "Женский", "Не указан"
    val avatarFrameUri: String = "", // Рамка аватара пользователя
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
