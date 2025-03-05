package org.orgaprop.controlprop.utils.prefs.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.utils.prefs.models.Pref
import org.orgaprop.controlprop.utils.prefs.repository.PrefRepository
import org.orgaprop.controlprop.utils.types.Result

class PrefViewModel(private val prefRepository: PrefRepository) : ViewModel() {

    // État pour stocker la préférence actuelle
    private val _pref = MutableStateFlow<Pref?>(null)
    val pref: StateFlow<Pref?> = _pref

    // État pour stocker les messages d'erreur
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Récupérer une préférence par son paramètre
    fun getPrefFromParam(param: String) {
        viewModelScope.launch {
            prefRepository.getPrefFromParam(param).collect { pref ->
                _pref.value = pref
            }
        }
    }

    // Insérer une préférence
    fun insertPref(pref: Pref) {
        viewModelScope.launch {
            when (val result = prefRepository.insertPref(pref)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec la nouvelle préférence
                    _pref.value = pref
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Mettre à jour une préférence
    fun updatePref(pref: Pref) {
        viewModelScope.launch {
            when (val result = prefRepository.updatePref(pref)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec la préférence mise à jour
                    _pref.value = pref
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer une préférence par son paramètre
    fun deletePref(param: String) {
        viewModelScope.launch {
            when (val result = prefRepository.deletePref(param)) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état de la préférence
                    _pref.value = null
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer une préférence par son ID
    fun deletePref(paramId: Long) {
        viewModelScope.launch {
            when (val result = prefRepository.deletePref(paramId)) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état de la préférence
                    _pref.value = null
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
