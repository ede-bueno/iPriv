package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections WHERE deletedAt IS NULL ORDER BY title ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getCollectionById(id: String): Flow<CollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("UPDATE collections SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteCollection(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)
}
