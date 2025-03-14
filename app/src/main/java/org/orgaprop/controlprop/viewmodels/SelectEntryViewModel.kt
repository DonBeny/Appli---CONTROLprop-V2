package org.orgaprop.controlprop.viewmodels

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.managers.SelectEntryManager
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity

class SelectEntryViewModel(
    private val selectEntryManager: SelectEntryManager
) : ViewModel() {

    private var idMbr: Int = -1
    private var adrMac: String = ""

    // LiveData pour les sélections
    private val _selectedAgence = MutableLiveData<String>()
    val selectedAgence: LiveData<String> get() = _selectedAgence

    private val _selectedAgenceId = MutableLiveData<Int?>()
    val selectedAgenceId: LiveData<Int?> get() = _selectedAgenceId

    private val _selectedGroupement = MutableLiveData<String>()
    val selectedGroupement: LiveData<String> get() = _selectedGroupement

    private val _selectedGroupementId = MutableLiveData<Int?>()
    val selectedGroupementId: LiveData<Int?> get() = _selectedGroupementId

    private val _selectedResidence = MutableLiveData<String>()
    val selectedResidence: LiveData<String> get() = _selectedResidence

    private val _selectedResidenceId = MutableLiveData<Int?>()
    val selectedResidenceId: LiveData<Int?> get() = _selectedResidenceId

    // LiveData pour les cases à cocher
    private val _isProximityChecked = MutableLiveData<Boolean>(true)
    val isProximityChecked: LiveData<Boolean> get() = _isProximityChecked

    private val _isContractChecked = MutableLiveData<Boolean>(true)
    val isContractChecked: LiveData<Boolean> get() = _isContractChecked

    private val _navigateToSearch = MutableSharedFlow<String>()
    val navigateToSearch: SharedFlow<String> get() = _navigateToSearch

    // LiveData pour fermer l'application
    private val _navigateToCloseApp = MutableLiveData<Boolean>()
    val navigateToCloseApp: LiveData<Boolean> get() = _navigateToCloseApp

    // LiveData pour les erreurs
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun onLogoutButtonClicked() {
        viewModelScope.launch {
            try {
                // Appeler la méthode de déconnexion dans SelectEntryManager
                selectEntryManager.logout(idMbr, adrMac)

                // Fermer l'application après une déconnexion réussie
                _navigateToCloseApp.value = true
            } catch (e: Exception) {
                // En cas d'erreur, afficher un message d'erreur
                _errorMessage.value = "Erreur lors de la déconnexion : ${e.message}"
                // Fermer l'application même en cas d'erreur
                _navigateToCloseApp.value = true
            }
        }
    }

    fun onSearchButtonClicked(query: String) {
        if (query.isNotEmpty()) {
            // Émettez la requête de recherche pour que l'activité puisse gérer la navigation
            viewModelScope.launch {
                _navigateToSearch.emit(query)
            }
        } else {
            _errorMessage.value = "Veuillez saisir un terme de recherche"
        }
    }

    fun onProximityCheckChanged(isChecked: Boolean) {
        _isProximityChecked.value = isChecked
    }

    fun onContractCheckChanged(isChecked: Boolean) {
        _isContractChecked.value = isChecked
    }

    fun onNextButtonClicked() {
        val residence = _selectedResidence.value
        val isProximityChecked = _isProximityChecked.value ?: false
        val isContractChecked = _isContractChecked.value ?: false

        if (residence.isNullOrEmpty()) {
            _errorMessage.value = "Veuillez sélectionner une résidence"
        } else if (!isProximityChecked && !isContractChecked) {
            _errorMessage.value = "Veuillez cocher au moins une option (Proximité ou Contrat)"
        } else {
            // Toutes les conditions sont remplies, passer à l'écran suivant
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        // TODO: Implémenter la navigation vers l'écran suivant
        // Par exemple, lancer une nouvelle activité ou un fragment
        _errorMessage.value = "Navigation vers l'écran suivant"
    }

    // Méthode pour gérer les éléments sélectionnés
    fun handleSelectedItem(item: SelectItem) {
        when (item.type) {
            SelectListActivity.SELECT_LIST_TYPE_AGC -> {
                _selectedAgence.value = item.name
                _selectedAgenceId.value = item.id
            }
            SelectListActivity.SELECT_LIST_TYPE_GRP -> {
                _selectedGroupement.value = item.name
                _selectedGroupementId.value = item.id
            }
            SelectListActivity.SELECT_LIST_TYPE_RSD -> {
                _selectedResidence.value = item.name
                _selectedResidenceId.value = item.id
            }
        }
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac
    }

}
