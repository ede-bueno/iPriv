package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.ScrapbookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapbookDao {

    @Query("SELECT * FROM scrapbooks ORDER BY updatedAt DESC")
    fun getAllScrapbooks(): Flow<List<ScrapbookEntity>>

    @Query("SELECT * FROM scrapbooks")
    suspend fun getAllScrapbooksDirect(): List<ScrapbookEntity>

    @Query("SELECT * FROM scrapbooks WHERE id = :id LIMIT 1")
    suspend fun getScrapbookById(id: String): ScrapbookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrapbook(scrapbook: ScrapbookEntity)

    @Update
    suspend fun updateScrapbook(scrapbook: ScrapbookEntity)

    @Query("DELETE FROM scrapbooks WHERE id = :id")
    suspend fun deleteScrapbook(id: String)
}
