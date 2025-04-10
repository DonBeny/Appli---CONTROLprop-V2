package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch

import org.json.JSONException
import org.json.JSONObject

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectEntry.SelectListActivity
import org.orgaprop.controlprop.ui.selectEntry.parsers.JsonParser

class SelectListViewModel(
    private val selectListManager: SelectListManager
): ViewModel() {

    companion object {
        private const val TAG = "SelectListViewModel"
    }

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private lateinit var userData: LoginData

    private val _items = MutableLiveData<List<SelectItem>>()
    val items: LiveData<List<SelectItem>> get() = _items

    private val _listAgents = MutableLiveData<JSONObject>()
    val listAgents: LiveData<JSONObject> get() = _listAgents

    private val _listPrestataires = MutableLiveData<JSONObject>()
    val listPrestataires: LiveData<JSONObject> get() = _listPrestataires

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading


    /**
     * Récupère les données correspondant au type et aux paramètres
     * @param type Type de liste (agc, grp, rsd, search)
     * @param parentId ID du parent (agence ou groupement)
     * @param searchQuery Termes de recherche (pour le type search)
     */
    fun fetchData(type: String, parentId: Int, searchQuery: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val items = when (type) {
                    SelectListActivity.SELECT_LIST_TYPE_AGC -> fetchAgenciesFromUserData()
                    else -> {
                        val jsonResponse = selectListManager.fetchData(
                            type,
                            parentId,
                            searchQuery,
                            idMbr,
                            adrMac
                        )
                        parseJsonResponse(jsonResponse, type)
                    }
                }
                _items.value = items
                _isLoading.value = false
            } catch (e: BaseException) {
                _errorMessage.value = e.message
                _isLoading.value = false
            } catch (e: JSONException) {
                _errorMessage.value = "Erreur de traitement des données"
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data: ${e.message}", e)
                _errorMessage.value = "Une erreur inconnue s'est produite"
                _isLoading.value = false
            }
        }
    }

    /**
     * Récupère la liste des agences à partir des données utilisateur
     * @return Liste des agences sous forme de SelectItem
     */
    private fun fetchAgenciesFromUserData(): List<SelectItem> {
        return userData.let {
            val agenciesArray = it.agencies
            val agencies = mutableListOf<SelectItem>()

            for (agency in agenciesArray) {
                agencies.add(
                    SelectItem(
                        id = agency.id,
                        name = agency.nom,
                        type = SelectListActivity.SELECT_LIST_TYPE_AGC
                    )
                )
            }
            agencies
        }
    }



    /**
     * Analyse la réponse JSON en fonction du type de liste
     * @param jsonResponse Réponse JSON du serveur
     * @param type Type de liste (grp, rsd, search)
     * @return Liste d'éléments SelectItem
     */
    private fun parseJsonResponse(jsonResponse: JSONObject, type: String): List<SelectItem> {
        if (jsonResponse.getBoolean("status")) {
            if (type == SelectListActivity.SELECT_LIST_TYPE_GRP) {
                val items = JsonParser.parseResponseGrp(jsonResponse, type)

                // Extraire les agents et prestataires pour les stocker dans les LiveData
                val (agents, prestataires) = JsonParser.extractAgentsAndPrestataires(jsonResponse)
                _listAgents.postValue(agents)
                _listPrestataires.postValue(prestataires)

                return items
            } else {
                return JsonParser.parseResponseRsd(jsonResponse, type)
            }
        } else {
            val errorMessage = jsonResponse.optJSONObject("error")?.optString("txt")
                ?: "Erreur inconnue"
            _errorMessage.postValue(errorMessage)
        }

        return emptyList()
    }



    /**
     * Définit les données utilisateur
     * @param userData Données utilisateur
     */
    fun setUserData(userData: LoginData) {
        this.userData = userData
        idMbr = userData.idMbr
        adrMac = userData.adrMac
    }

}
