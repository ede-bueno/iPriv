package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getPersonById(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getPersonByIdDirect(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("UPDATE persons SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeletePerson(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePerson(id: String)
}
