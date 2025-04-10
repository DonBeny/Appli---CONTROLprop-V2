package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.orgaprop.controlprop.managers.SelectEntryManager
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectEntry.SelectListActivity

class SelectEntryViewModel(
    private val selectEntryManager: SelectEntryManager
) : ViewModel() {

    companion object {
        private const val TAG = "SelectEntryViewModel"
    }

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

    private val _isProximityChecked = MutableLiveData(true)
    val isProximityChecked: LiveData<Boolean> get() = _isProximityChecked

    private val _isContractChecked = MutableLiveData(true)
    val isContractChecked: LiveData<Boolean> get() = _isContractChecked

    private val _navigateToSearch = MutableSharedFlow<String>(replay = 1)
    val navigateToSearch: SharedFlow<String> get() = _navigateToSearch

    private val _navigateToCloseApp = MutableLiveData<Boolean>()
    val navigateToCloseApp: LiveData<Boolean> get() = _navigateToCloseApp

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage



    fun onLogoutButtonClicked() {
        viewModelScope.launch {
            try {
                selectEntryManager.logout(idMbr, adrMac)

                _navigateToCloseApp.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors de la déconnexion : ${e.message}"
                _navigateToCloseApp.value = true
            }
        }
    }

    fun onSearchButtonClicked(query: String) {
        if (query.isNotEmpty()) {
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
            //TODO : Enregistrer l'entrée sélectionnée et la dernière liste des entrées reçues
            //_navigateToNextScreen.value = true
        }
    }



    fun handleSelectedItem(item: SelectItem) {
        //Log.d(TAG, "handleSelectedItem: Item ${item.type} selected: ${item.name}")

        when (item.type) {
            SelectListActivity.SELECT_LIST_TYPE_AGC -> {
                //Log.d(TAG, "handleSelectedItem: Selected Agence: ${item.name}")

                _selectedAgence.value = item.name
                _selectedAgenceId.value = item.id

                _selectedGroupement.value = ""
                _selectedGroupementId.value = null
                _selectedResidence.value = ""
                _selectedResidenceId.value = null
            }
            SelectListActivity.SELECT_LIST_TYPE_GRP -> {
                //Log.d(TAG, "handleSelectedItem: Selected Groupement: ${item.name}")

                _selectedGroupement.value = item.name
                _selectedGroupementId.value = item.id

                _selectedResidence.value = ""
                _selectedResidenceId.value = null
            }
            SelectListActivity.SELECT_LIST_TYPE_RSD -> {
                //Log.d(TAG, "handleSelectedItem: Selected Residence: ${item.name}")

                _selectedResidence.value = item.ref + " -- " + item.name + " -- Entrée " + item.entry
                _selectedResidenceId.value = item.id

                // Accéder aux données prop
                /*item.prop?.let { prop ->
                    //Log.d(TAG, "Proxi zones: ${prop.zones.proxi}")
                    //Log.d(TAG, "Contra zones: ${prop.zones.contra}")
                    //Log.d(TAG, "Ctrl note: ${prop.ctrl.note}")

                    // Convertir la chaîne JSON en JSONArray
                    val grille = prop.ctrl.getGrilleAsJsonArray()
                    //Log.d(TAG, "Grille: $grille")
                }*/
            }
            SelectListActivity.SELECT_LIST_TYPE_SEARCH -> {
                handleSearchItem(item)
            }
        }
    }

    /**
     * Traitement spécifique pour les résultats de recherche
     * @param item Élément de recherche sélectionné
     */
    private fun handleSearchItem(item: SelectItem) {
        try {
            val t = JSONObject(item.comment)

            _selectedAgence.value = t.getJSONObject("agency").getString("txt")
            _selectedAgenceId.value = t.getJSONObject("agency").getInt("id")

            _selectedGroupement.value = t.getJSONObject("group").getString("txt")
            _selectedGroupementId.value = t.getJSONObject("group").getInt("id")

            _selectedResidence.value = "${item.ref} -- ${item.name} -- Entrée ${item.entry}"
            _selectedResidenceId.value = item.id
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing search result: ${e.message}")
            _errorMessage.value = "Erreur lors du traitement du résultat de recherche"
        }
    }



    /**
     * Définit un message d'erreur
     * @param message Message d'erreur
     */
    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    /**
     * Définit les informations d'identification de l'utilisateur
     * @param idMbr ID de l'utilisateur
     * @param adrMac Adresse MAC de l'appareil
     */
    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        Log.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
    }

}
