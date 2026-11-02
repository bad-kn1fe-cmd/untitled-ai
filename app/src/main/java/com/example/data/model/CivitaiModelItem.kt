package com.example.data.model

data class CivitaiModelItem(
    val id: Long,
    val name: String,
    val type: String = "Checkpoint",
    val description: String = "",
    val versions: List<CivitaiVersionItem> = emptyList()
)

data class CivitaiVersionItem(
    val id: Long,
    val name: String,
    val baseModel: String,
    val air: String,
    val previewImageUrl: String? = null
)
