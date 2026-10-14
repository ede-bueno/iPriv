package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.TimeCapsuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeCapsuleDao {

    @Query("SELECT * FROM time_capsules ORDER BY unlockTimestamp ASC")
    fun getAllCapsules(): Flow<List<TimeCapsuleEntity>>

    @Query("SELECT * FROM time_capsules WHERE id = :id LIMIT 1")
    suspend fun getCapsuleById(id: String): TimeCapsuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsule(capsule: TimeCapsuleEntity)

    @Update
    suspend fun updateCapsule(capsule: TimeCapsuleEntity)

    @Query("UPDATE time_capsules SET isLocked = 0, openedAt = :openedAt WHERE id = :id")
    suspend fun unlockCapsule(id: String, openedAt: Long)

    @Query("DELETE FROM time_capsules WHERE id = :id")
    suspend fun deleteCapsule(id: String)
}
