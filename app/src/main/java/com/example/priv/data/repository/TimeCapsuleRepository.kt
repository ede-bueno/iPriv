package com.example.priv.data.repository

import com.example.priv.data.dao.TimeCapsuleDao
import com.example.priv.data.entity.TimeCapsuleEntity
import com.example.priv.data.model.CapsuleMediaItem
import kotlinx.coroutines.flow.Flow

class TimeCapsuleRepository(private val capsuleDao: TimeCapsuleDao) {

    val allCapsules: Flow<List<TimeCapsuleEntity>> = capsuleDao.getAllCapsules()

    suspend fun getCapsuleById(id: String): TimeCapsuleEntity? {
        return capsuleDao.getCapsuleById(id)
    }

    suspend fun createCapsule(
        title: String,
        note: String,
        unlockTimestamp: Long,
        memoryIds: List<String>,
        mediaItems: List<CapsuleMediaItem> = emptyList(),
        targetPersonId: String? = null,
        themeColorHex: String = "#7C4DFF"
    ): TimeCapsuleEntity {
        val memoryIdsJson = "[" + memoryIds.joinToString(",") { "\"$it\"" } + "]"
        val mediaItemsJson = CapsuleMediaItem.listToJsonString(mediaItems)
        val capsule = TimeCapsuleEntity(
            title = title,
            note = note,
            unlockTimestamp = unlockTimestamp,
            isLocked = System.currentTimeMillis() < unlockTimestamp,
            memoryIdsJson = memoryIdsJson,
            mediaItemsJson = mediaItemsJson,
            targetPersonId = targetPersonId,
            themeColorHex = themeColorHex
        )
        capsuleDao.insertCapsule(capsule)
        return capsule
    }

    suspend fun unlockCapsule(id: String) {
        capsuleDao.unlockCapsule(id, System.currentTimeMillis())
    }

    suspend fun deleteCapsule(id: String) {
        capsuleDao.deleteCapsule(id)
    }
}
