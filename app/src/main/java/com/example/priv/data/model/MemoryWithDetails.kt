package com.example.priv.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.priv.data.entity.*

data class MemoryWithDetails(
    @Embedded val memory: MemoryEntity,

    @Relation(
        parentColumn = "primaryPersonId",
        entityColumn = "id"
    )
    val primaryPerson: PersonEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "memoryId"
    )
    val attachments: List<MemoryAttachmentEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryPersonCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "personId"
        )
    )
    val taggedPersons: List<PersonEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryMomentCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "momentId"
        )
    )
    val moments: List<MomentEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryCollectionCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "collectionId"
        )
    )
    val collections: List<CollectionEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryTagCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
