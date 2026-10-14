package com.example.priv.data.repository

import com.example.priv.data.dao.GroupDao
import com.example.priv.data.dao.SyncQueueDao
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.GroupPersonCrossRef
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.entity.SyncOperationEntity
import com.example.priv.data.model.GroupWithDetails
import com.example.priv.data.model.OperationType
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GroupRepository(
    private val groupDao: GroupDao,
    private val syncQueueDao: SyncQueueDao? = null
) {
    val allGroupsWithDetails: Flow<List<GroupWithDetails>> = groupDao.getAllGroupsWithDetails()
    val allGroups: Flow<List<GroupEntity>> = groupDao.getAllGroups()

    fun getGroupByIdWithDetails(id: String): Flow<GroupWithDetails?> = groupDao.getGroupByIdWithDetails(id)
    suspend fun getGroupByIdDirect(id: String): GroupEntity? = groupDao.getGroupByIdDirect(id)

    suspend fun createGroup(
        name: String,
        description: String = "",
        colorHex: String = "#8E3BEE",
        iconName: String = "Group",
        memberPersonIds: List<String> = emptyList(),
        spaceId: String = "default_space"
    ): String {
        val groupId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val group = GroupEntity(
            id = groupId,
            spaceId = spaceId,
            name = name,
            description = description,
            colorHex = colorHex,
            iconName = iconName,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY
        )

        groupDao.insertGroup(group)

        for (personId in memberPersonIds.distinct()) {
            groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(groupId, personId))
        }

        syncQueueDao?.enqueueOperation(
            SyncOperationEntity(
                entityType = "GROUP",
                entityId = groupId,
                operationType = OperationType.CREATE_ENTITY,
                payloadJson = """{"id":"$groupId","name":"$name"}""",
                status = SyncStatus.PENDING_CREATE
            )
        )

        return groupId
    }

    suspend fun updateGroup(group: GroupEntity, memberPersonIds: List<String>? = null) {
        val updated = group.copy(updatedAt = System.currentTimeMillis())
        groupDao.updateGroup(updated)

        if (memberPersonIds != null) {
            groupDao.clearGroupMembers(group.id)
            for (pId in memberPersonIds.distinct()) {
                groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(group.id, pId))
            }
        }
    }

    suspend fun addPersonToGroup(groupId: String, personId: String) {
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(groupId, personId))
    }

    suspend fun removePersonFromGroup(groupId: String, personId: String) {
        groupDao.removePersonFromGroup(groupId, personId)
    }

    suspend fun deleteGroup(groupId: String) {
        // Deleting group removes group record and memberships, but preserves persons and memories
        groupDao.clearGroupMembers(groupId)
        groupDao.softDeleteGroup(groupId)
    }
}
