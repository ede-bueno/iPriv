package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.GroupPersonCrossRef
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.GroupWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Transaction
    @Query("SELECT * FROM groups WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>>

    @Query("SELECT * FROM groups WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAllGroupsList(): List<GroupEntity>

    @Transaction
    @Query("SELECT * FROM groups WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getGroupByIdWithDetails(id: String): Flow<GroupWithDetails?>

    @Query("SELECT * FROM groups WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getGroupByIdDirect(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("UPDATE groups SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteGroup(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPersonCrossRef(crossRef: GroupPersonCrossRef)

    @Query("DELETE FROM group_person_cross_ref WHERE groupId = :groupId AND personId = :personId")
    suspend fun removePersonFromGroup(groupId: String, personId: String)

    @Query("DELETE FROM group_person_cross_ref WHERE groupId = :groupId")
    suspend fun clearGroupMembers(groupId: String)

    @Query("SELECT p.* FROM persons p INNER JOIN group_person_cross_ref gp ON p.id = gp.personId WHERE gp.groupId = :groupId AND p.deletedAt IS NULL")
    fun getMembersForGroup(groupId: String): Flow<List<PersonEntity>>
}
