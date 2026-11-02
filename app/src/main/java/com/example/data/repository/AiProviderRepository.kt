package com.example.data.repository

import com.example.ai.engine.AiProviderEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiProviderProfileEntity
import kotlinx.coroutines.flow.Flow

class AiProviderRepository(private val db: AppDatabase) {

    val allProfiles: Flow<List<AiProviderProfileEntity>> = db.aiProviderDao().getAllProfiles()
    val defaultTextProfile: Flow<AiProviderProfileEntity?> = db.aiProviderDao().getDefaultTextProfileFlow()
    val defaultImageProfile: Flow<AiProviderProfileEntity?> = db.aiProviderDao().getDefaultImageProfileFlow()

    suspend fun getProfileById(id: Long): AiProviderProfileEntity? = db.aiProviderDao().getProfileById(id)

    suspend fun saveProfile(profile: AiProviderProfileEntity): Long {
        return if (profile.id == 0L) {
            val id = db.aiProviderDao().insertProfile(profile)
            if (profile.isDefaultText) db.aiProviderDao().setDefaultTextProfile(id)
            if (profile.isDefaultImage) db.aiProviderDao().setDefaultImageProfile(id)
            id
        } else {
            db.aiProviderDao().updateProfile(profile)
            if (profile.isDefaultText) db.aiProviderDao().setDefaultTextProfile(profile.id)
            if (profile.isDefaultImage) db.aiProviderDao().setDefaultImageProfile(profile.id)
            profile.id
        }
    }

    suspend fun deleteProfile(profile: AiProviderProfileEntity) = db.aiProviderDao().deleteProfile(profile)

    suspend fun setDefaultText(id: Long) = db.aiProviderDao().setDefaultTextProfile(id)
    suspend fun setDefaultImage(id: Long) = db.aiProviderDao().setDefaultImageProfile(id)

    suspend fun testProfileConnection(profile: AiProviderProfileEntity): Result<String> {
        return AiProviderEngine.testConnection(profile)
    }

    suspend fun fetchRemoteModels(providerType: String, baseUrl: String = "", apiKey: String = ""): Result<List<String>> {
        return AiProviderEngine.fetchRemoteModels(providerType, baseUrl, apiKey)
    }

    suspend fun fetchOpenRouterModels(apiKey: String): Result<List<String>> {
        return AiProviderEngine.fetchOpenRouterModels(apiKey)
    }

    suspend fun searchCivitaiModels(query: String, apiKey: String = "") =
        AiProviderEngine.searchCivitaiModels(query, apiKey)

    suspend fun generateImage(
        profile: AiProviderProfileEntity?,
        prompt: String,
        negativePrompt: String = "",
        resolution: String = "832x1216",
        engineMode: String = "auto"
    ) = AiProviderEngine.generateImage(profile, prompt, negativePrompt, resolution, engineMode)
}

