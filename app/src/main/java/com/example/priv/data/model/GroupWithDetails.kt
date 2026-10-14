package com.example.priv.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.GroupPersonCrossRef
import com.example.priv.data.entity.PersonEntity

data class GroupWithDetails(
    @Embedded val group: GroupEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GroupPersonCrossRef::class,
            parentColumn = "groupId",
            entityColumn = "personId"
        )
    )
    val members: List<PersonEntity> = emptyList()
)
