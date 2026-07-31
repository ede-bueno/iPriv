package com.example.priv.data.sync

import com.example.priv.data.entity.MemoryEntity

interface ConflictResolver {
    fun resolveMemoryConflict(local: MemoryEntity, remote: MemoryEntity): MemoryEntity
}

class LastWriteWinsConflictResolver : ConflictResolver {
    override fun resolveMemoryConflict(local: MemoryEntity, remote: MemoryEntity): MemoryEntity {
        // Last Write Wins based on updatedAt timestamp
        return if ((local.updatedAt) >= (remote.updatedAt)) {
            local
        } else {
            remote
        }
    }
}
