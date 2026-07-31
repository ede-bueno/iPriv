package com.example.priv.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "memory_person_cross_ref",
    primaryKeys = ["memoryId", "personId"],
    indices = [Index(value = ["personId"])]
)
data class MemoryPersonCrossRef(
    val memoryId: String,
    val personId: String
)

@Entity(
    tableName = "memory_moment_cross_ref",
    primaryKeys = ["memoryId", "momentId"],
    indices = [Index(value = ["momentId"])]
)
data class MemoryMomentCrossRef(
    val memoryId: String,
    val momentId: String
)

@Entity(
    tableName = "memory_collection_cross_ref",
    primaryKeys = ["memoryId", "collectionId"],
    indices = [Index(value = ["collectionId"])]
)
data class MemoryCollectionCrossRef(
    val memoryId: String,
    val collectionId: String
)

@Entity(
    tableName = "memory_tag_cross_ref",
    primaryKeys = ["memoryId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class MemoryTagCrossRef(
    val memoryId: String,
    val tagId: String
)

@Entity(
    tableName = "group_person_cross_ref",
    primaryKeys = ["groupId", "personId"],
    indices = [Index(value = ["personId"])]
)
data class GroupPersonCrossRef(
    val groupId: String,
    val personId: String
)
