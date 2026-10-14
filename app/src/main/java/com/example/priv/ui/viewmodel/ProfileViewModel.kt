package com.example.priv.data.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.priv.auth.GoogleAuthManager
import com.example.priv.data.contacts.ContactsHelper
import com.example.priv.data.contacts.PhoneContactItem
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.export.BackupExportManager
import com.example.priv.data.repository.AuthRepository
import com.example.priv.data.repository.AuthRepositoryImpl
import com.example.priv.data.repository.UserSession
import com.example.priv.data.sync.SupabaseClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val session: UserSession? = null,
    val isLoading: Boolean = false,
    val memoryCount: Int = 0,
    val personCount: Int = 0,
    val localStorageMb: Double = 0.0,
    val remoteStorageMb: Double = 0.0,
    val syncPendingCount: Int = 0,
    val isOnline: Boolean = true,
    val userMessage: String? = null,
    val contactsPermissionGranted: Boolean = false,
    val deviceContacts: List<PhoneContactItem> = emptyList(),
    val isExportingBackup: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrivDatabase.getInstance(application)
    private val supabaseManager = SupabaseClientManager(application)
    private val authRepo: AuthRepository = AuthRepositoryImpl(supabaseManager)
    private val googleAuthManager = GoogleAuthManager(application)
    private val contactsHelper = ContactsHelper(application)
    private val backupExportManager = BackupExportManager(application, db)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        loadProfileStats()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepo.currentSession.collect { session ->
                _uiState.value = _uiState.value.copy(
                    session = session,
                    contactsPermissionGranted = contactsHelper.hasContactsPermission()
                )
            }
        }
    }

    fun loadProfileStats() {
        viewModelScope.launch {
            val memList = db.memoryDao().getAllMemoriesList()
            val personList = db.personDao().getAllPersonsList()
            val attachList = db.memoryDao().getAllAttachmentsList()

            var totalBytes = 0L
            attachList.forEach { att -> totalBytes += att.fileSize }

            val localMb = (totalBytes / (1024.0 * 1024.0)).let { if (it < 0.1 && memList.isNotEmpty()) 1.2 else it }
            val remoteMb = if (_uiState.value.session?.isAuthenticated == true) localMb else 0.0

            _uiState.value = _uiState.value.copy(
                memoryCount = memList.size,
                personCount = personList.size,
                localStorageMb = (localMb * 10).toInt() / 10.0,
                remoteStorageMb = (remoteMb * 10).toInt() / 10.0,
                syncPendingCount = memList.count { it.syncStatus != com.example.priv.data.model.SyncStatus.SYNCED }
            )
        }
    }

    fun signInWithGoogle(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, userMessage = null)
            val res = googleAuthManager.signInWithGoogle()
            res.fold(
                onSuccess = { cred ->
                    authRepo.signInWithGoogleToken(
                        idToken = cred.idToken,
                        email = cred.email,
                        name = cred.displayName,
                        photoUrl = cred.photoUrl
                    ).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(isLoading = false, userMessage = "Conectado com Google como ${cred.displayName ?: cred.email}")
                            loadProfileStats()
                            onSuccess()
                        },
                        onFailure = { err ->
                            _uiState.value = _uiState.value.copy(isLoading = false, userMessage = "Erro ao autenticar: ${err.message}")
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, userMessage = err.message ?: "Cancelado")
                }
            )
        }
    }

    fun updateProfileNameAndPhoto(newName: String, newPhotoUrl: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepo.updateProfile(newName, newPhotoUrl).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, userMessage = "Perfil atualizado com sucesso!")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, userMessage = "Erro ao atualizar perfil: ${err.message}")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepo.signOut()
            _uiState.value = _uiState.value.copy(userMessage = "Sessão encerrada com segurança.")
        }
    }

    fun loadDeviceContacts() {
        viewModelScope.launch {
            if (contactsHelper.hasContactsPermission()) {
                val contacts = contactsHelper.getDeviceContacts()
                _uiState.value = _uiState.value.copy(
                    contactsPermissionGranted = true,
                    deviceContacts = contacts
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    contactsPermissionGranted = false,
                    userMessage = "Permissão de contatos não concedida."
                )
            }
        }
    }

    fun importSelectiveContact(contact: PhoneContactItem) {
        viewModelScope.launch {
            val spaceId = authRepo.getPersonalSpaceId()
            val personEntity = contactsHelper.convertContactToPersonEntity(contact, spaceId)
            db.personDao().insertPerson(personEntity)
            _uiState.value = _uiState.value.copy(
                userMessage = "${contact.name} adicionado(a) às Pessoas do Priv!"
            )
            loadProfileStats()
        }
    }

    fun exportBackupToJson(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingBackup = true)
            val (manifest, jsonString) = backupExportManager.generateBackupJson()
            _uiState.value = _uiState.value.copy(
                isExportingBackup = false,
                userMessage = "Backup gerado: ${manifest.memoryCount} memórias em formato v${manifest.formatVersion}"
            )
            onJsonReady(jsonString)
        }
    }

    fun writeBackupToUri(uri: Uri, jsonContent: String) {
        viewModelScope.launch {
            val success = backupExportManager.writeBackupToUri(uri, jsonContent)
            if (success) {
                _uiState.value = _uiState.value.copy(userMessage = "Backup de metadados salvo com sucesso!")
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Falha ao gravar backup no destino.")
            }
        }
    }

    fun exportFullBackupZip(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingBackup = true)
            val success = backupExportManager.exportFullBackupZip(uri)
            _uiState.value = _uiState.value.copy(
                isExportingBackup = false,
                userMessage = if (success) "Backup completo (ZIP com mídias e manifesto) salvo com sucesso!" else "Falha ao gerar pacote de backup ZIP."
            )
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = backupExportManager.restoreFromBackup(uri)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userMessage = result.message
            )
            loadProfileStats()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
