package com.example.priv.data.repository

import com.example.priv.data.dao.CollectionDao
import com.example.priv.data.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {
    val allCollections: Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    fun getCollectionById(id: String): Flow<CollectionEntity?> = collectionDao.getCollectionById(id)

    suspend fun insertCollection(collection: CollectionEntity) = collectionDao.insertCollection(collection)
    suspend fun updateCollection(collection: CollectionEntity) = collectionDao.updateCollection(collection)
    suspend fun deleteCollection(id: String) = collectionDao.softDeleteCollection(id)
}
