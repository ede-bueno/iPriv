package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.priv.data.model.MemberStatus
import com.example.priv.data.model.RoleType
import java.util.UUID

@Entity(
    tableName = "space_members",
    indices = [Index(value = ["spaceId"]), Index(value = ["userId"])]
)
data class SpaceMemberEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val spaceId: String,
    val userId: String,
    val role: RoleType = RoleType.OWNER,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val invitedByUserId: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
)
