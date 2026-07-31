package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.MomentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {

    @Query("SELECT * FROM moments WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllMoments(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getMomentById(id: String): Flow<MomentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity)

    @Update
    suspend fun updateMoment(moment: MomentEntity)

    @Query("UPDATE moments SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMoment(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM moments WHERE id = :id")
    suspend fun deleteMoment(id: String)
}
