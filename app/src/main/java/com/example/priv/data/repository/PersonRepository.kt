package com.example.priv.data.repository

import com.example.priv.data.dao.PersonDao
import com.example.priv.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

class PersonRepository(private val personDao: PersonDao) {
    val allPersons: Flow<List<PersonEntity>> = personDao.getAllPersons()

    fun getPersonById(id: String): Flow<PersonEntity?> = personDao.getPersonById(id)
    suspend fun getPersonByIdDirect(id: String): PersonEntity? = personDao.getPersonByIdDirect(id)

    suspend fun insertPerson(person: PersonEntity) = personDao.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = personDao.updatePerson(person)
    suspend fun deletePerson(id: String) = personDao.softDeletePerson(id)
}
