package com.example.priv.data.repository

import com.example.priv.data.dao.MomentDao
import com.example.priv.data.entity.MomentEntity
import kotlinx.coroutines.flow.Flow

class MomentRepository(private val momentDao: MomentDao) {
    val allMoments: Flow<List<MomentEntity>> = momentDao.getAllMoments()

    fun getMomentById(id: String): Flow<MomentEntity?> = momentDao.getMomentById(id)

    suspend fun insertMoment(moment: MomentEntity) = momentDao.insertMoment(moment)
    suspend fun updateMoment(moment: MomentEntity) = momentDao.updateMoment(moment)
    suspend fun deleteMoment(id: String) = momentDao.softDeleteMoment(id)
}
