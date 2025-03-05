package org.orgaprop.controlprop.utils.prefs.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.utils.prefs.models.Storage
import org.orgaprop.controlprop.utils.prefs.repository.StorageRepository
import org.orgaprop.controlprop.utils.types.Result

class StorageViewModel(private val storageRepository: StorageRepository) : ViewModel() {

    // État pour stocker la liste des enregistrements de stockage
    private val _storageList = MutableStateFlow<List<Storage>>(emptyList())
    val storageList: StateFlow<List<Storage>> = _storageList

    // État pour stocker l'enregistrement de stockage actuel
    private val _storage = MutableStateFlow<Storage?>(null)
    val storage: StateFlow<Storage?> = _storage

    // État pour stocker les messages d'erreur
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Récupérer tous les enregistrements de stockage
    fun getAllStorage() {
        viewModelScope.launch {
            storageRepository.getAllStorage().collect { storageList ->
                _storageList.value = storageList
            }
        }
    }

    // Récupérer un enregistrement de stockage par son ID
    fun getStorage(storageId: Int) {
        viewModelScope.launch {
            storageRepository.getStorage(storageId).collect { storage ->
                _storage.value = storage
            }
        }
    }

    // Récupérer un enregistrement de stockage par son resid
    fun getStorageRsd(storageRsd: Int) {
        viewModelScope.launch {
            storageRepository.getStorageRsd(storageRsd).collect { storage ->
                _storage.value = storage
            }
        }
    }

    // Insérer un enregistrement de stockage
    fun insertStorage(storage: Storage) {
        viewModelScope.launch {
            when (val result = storageRepository.insertStorage(storage)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec le nouvel enregistrement
                    _storage.value = storage
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Mettre à jour un enregistrement de stockage
    fun updateStorage(storage: Storage) {
        viewModelScope.launch {
            when (val result = storageRepository.updateStorage(storage)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec l'enregistrement mis à jour
                    _storage.value = storage
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer un enregistrement de stockage par son ID
    fun deleteStorageById(storageId: Long) {
        viewModelScope.launch {
            when (val result = storageRepository.deleteStorageById(storageId)) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état de l'enregistrement
                    _storage.value = null
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer un enregistrement de stockage par son resid
    fun deleteStorageByRsd(storageRsd: Long) {
        viewModelScope.launch {
            when (val result = storageRepository.deleteStorageByRsd(storageRsd)) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état de l'enregistrement
                    _storage.value = null
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer tous les enregistrements de stockage
    fun deleteAllStorage() {
        viewModelScope.launch {
            when (val result = storageRepository.deleteAllStorage()) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état de la liste
                    _storageList.value = emptyList()
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

}
