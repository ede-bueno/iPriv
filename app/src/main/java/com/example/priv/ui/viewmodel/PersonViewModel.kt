package com.example.priv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.data.repository.MemoryRepository
import com.example.priv.data.repository.PersonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PersonViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val personRepo = PersonRepository(db.personDao())
    private val memoryRepo = MemoryRepository(db.memoryDao(), db.tagDao(), db.syncQueueDao())

    val allPersons: StateFlow<List<PersonEntity>> = personRepo.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPersonId = MutableStateFlow<String?>(null)
    val selectedPersonId: StateFlow<String?> = _selectedPersonId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPersonMemories: StateFlow<List<MemoryWithDetails>> = _selectedPersonId
        .flatMapLatest { id ->
            if (id != null) {
                memoryRepo.getMemoriesForPerson(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPerson: StateFlow<PersonEntity?> = _selectedPersonId
        .flatMapLatest { id ->
            if (id != null) {
                personRepo.getPersonById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectPerson(personId: String?) {
        _selectedPersonId.value = personId
    }

    fun addPerson(name: String, nickname: String, relationship: String, colorHex: String, bioNote: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                personRepo.insertPerson(
                    PersonEntity(
                        name = name.trim(),
                        nickname = nickname.trim(),
                        relationship = relationship.trim().ifBlank { "Amigo(a)" },
                        colorHex = colorHex,
                        bioNote = bioNote.trim()
                    )
                )
            }
        }
    }

    fun toggleFavoriteMemory(memoryWithDetails: MemoryWithDetails) {
        viewModelScope.launch {
            memoryRepo.toggleFavorite(memoryWithDetails.memory)
        }
    }
}
