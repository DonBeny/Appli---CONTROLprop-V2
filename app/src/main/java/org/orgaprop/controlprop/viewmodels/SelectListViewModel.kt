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

    private val _complementaryDataReady = MutableLiveData<Boolean>()
    val complementaryDataReady: LiveData<Boolean> get() = _complementaryDataReady

    private val _complementaryDataError = MutableLiveData<String>()
    val complementaryDataError: LiveData<String> get() = _complementaryDataError


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

                        Log.d(TAG, "fetchData jsonResponse: $jsonResponse")

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
        if (!::userData.isInitialized) {
            Log.e(TAG, "fetchAgenciesFromUserData: userData not initialized")
            return emptyList()
        }

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
     * Récupère les données complémentaires (agents et prestataires) pour un groupe donné
     * @param groupId ID du groupe
     */
    fun fetchComplementaryData(agencyId: Int) {
        _isLoading.value = true
        _complementaryDataReady.value = false

        viewModelScope.launch {
            try {
                val jsonResponse = selectListManager.fetchData(
                    SelectListActivity.SELECT_LIST_TYPE_PRESTATES,
                    agencyId,
                    "",
                    idMbr,
                    adrMac
                )

                if (jsonResponse.getBoolean("status")) {
                    val (agents, prestataires) = JsonParser.extractAgentsAndPrestataires(jsonResponse)

                    _listAgents.postValue(agents)
                    _listPrestataires.postValue(prestataires)

                    _complementaryDataReady.postValue(true)
                } else {
                    val errorMessage = jsonResponse.optJSONObject("error")?.optString("txt")
                        ?: "Erreur lors de la récupération des données complémentaires"
                    _complementaryDataError.postValue(errorMessage)
                }

                _isLoading.postValue(false)
            } catch (e: BaseException) {
                _complementaryDataError.postValue(e.message)
                _isLoading.postValue(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching complementary data: ${e.message}", e)
                _complementaryDataError.postValue("Une erreur inconnue s'est produite")
                _isLoading.postValue(false)
            }
        }
    }



    fun setCachedItems(items: List<SelectItem>) {
        _items.value = items
        _isLoading.value = false
    }



    /**
     * Analyse la réponse JSON en fonction du type de liste
     * @param jsonResponse Réponse JSON du serveur
     * @param type Type de liste (grp, rsd, search)
     * @return Liste d'éléments SelectItem
     */
    private fun parseJsonResponse(jsonResponse: JSONObject, type: String): List<SelectItem> {
        if (jsonResponse.getBoolean("status")) {
            Log.d(TAG, "parseJsonResponse type: $type")

            if (type == SelectListActivity.SELECT_LIST_TYPE_GRP) {
                Log.d(TAG, "parseJsonResponse listGrp")

                val items = JsonParser.parseResponseGrp(jsonResponse, type)
                val (agents, prestataires) = JsonParser.extractAgentsAndPrestataires(jsonResponse)

                _listAgents.postValue(agents)
                _listPrestataires.postValue(prestataires)

                Log.d(TAG, "parseJsonResponse items: $items")
                Log.d(TAG, "parseJsonResponse agents: $agents")
                Log.d(TAG, "parseJsonResponse prestataires: $prestataires")

                return items
            } else {
                Log.d(TAG, "parseJsonResponse not listGrp")

                val items = JsonParser.parseResponseRsd(jsonResponse, type)

                Log.d(TAG, "parseJsonResponse items: $items")

                return items
            }
        } else {
            Log.e(TAG, "parseJsonResponse error serveur: ${jsonResponse.getJSONObject("error")}")
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
