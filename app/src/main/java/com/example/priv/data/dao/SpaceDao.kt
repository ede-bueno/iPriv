package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.SpaceEntity
import com.example.priv.data.entity.SpaceMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun getAllActiveSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces WHERE id = :id LIMIT 1")
    suspend fun getSpaceById(id: String): SpaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: SpaceMemberEntity)

    @Query("SELECT * FROM space_members WHERE spaceId = :spaceId")
    fun getMembersForSpace(spaceId: String): Flow<List<SpaceMemberEntity>>
}
