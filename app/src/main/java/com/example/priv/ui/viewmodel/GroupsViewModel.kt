package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.GroupWithDetails
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.GroupRepository
import com.example.priv.data.repository.MemoryRepository
import com.example.priv.data.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val groupRepo = GroupRepository(db.groupDao(), db.syncQueueDao())
    private val personRepo = PersonRepository(db.personDao())
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())

    val groups: StateFlow<List<GroupWithDetails>> = groupRepo.allGroupsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGroup(
        name: String,
        description: String = "",
        colorHex: String = "#8E3BEE",
        iconName: String = "Group",
        memberPersonIds: List<String> = emptyList(),
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = groupRepo.createGroup(
                name = name,
                description = description,
                colorHex = colorHex,
                iconName = iconName,
                memberPersonIds = memberPersonIds
            )
            onSuccess(id)
        }
    }

    fun updateGroup(group: GroupEntity, memberPersonIds: List<String>) {
        viewModelScope.launch {
            groupRepo.updateGroup(group, memberPersonIds)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepo.deleteGroup(groupId)
        }
    }

    fun getMemoriesForGroup(groupId: String): Flow<List<MemoryWithDetails>> {
        return memoryRepo.getMemoriesForGroup(groupId)
    }

    fun toggleFavorite(memory: MemoryWithDetails) {
        viewModelScope.launch {
            memoryRepo.toggleFavorite(memory.memory)
        }
    }

    fun moveToTrash(memoryId: String) {
        viewModelScope.launch {
            memoryRepo.moveToTrash(memoryId)
        }
    }
}
