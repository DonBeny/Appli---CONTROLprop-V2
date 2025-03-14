package org.orgaprop.controlprop.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity

class SelectListViewModel(
    private val selectListManager: SelectListManager
): ViewModel() {

    private val _items = MutableLiveData<List<SelectItem>>()
    val items: LiveData<List<SelectItem>> get() = _items

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private var userData: JSONObject? = null

    fun fetchData(type: String, parentId: Int, searchQuery: String, idMbr: Int, adrMac: String) {
        viewModelScope.launch {
            try {
                val items = when (type) {
                    SelectListActivity.SELECT_LIST_TYPE_AGC -> fetchAgencesFromUserData()
                    else -> {
                        val jsonResponse = selectListManager.fetchData(type, parentId, searchQuery, idMbr, adrMac)
                        parseJsonResponse(jsonResponse, type)
                    }
                }
                _items.value = items
            } catch (e: BaseException) {
                _errorMessage.value = e.message
            } catch (e: JSONException) {
                _errorMessage.value = "Erreur de traitement des données"
            } catch (e: Exception) {
                _errorMessage.value = "Une erreur inconnue s'est produite"
            }
        }
    }

    private fun fetchAgencesFromUserData(): List<SelectItem> {
        return userData?.let {
            val agencesArray = it.getJSONArray("agences")
            val agences = mutableListOf<SelectItem>()
            for (i in 0 until agencesArray.length()) {
                val agence = agencesArray.getJSONObject(i)
                agences.add(
                    SelectItem(
                        id = agence.getInt("id"),
                        name = agence.getString("name"),
                        type = SelectListActivity.SELECT_LIST_TYPE_AGC
                    )
                )
            }
            agences
        } ?: emptyList()
    }

    private fun parseJsonResponse(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val items = mutableListOf<SelectItem>()

        if (jsonResponse.getBoolean("status")) {
            val dataObject = jsonResponse.getJSONObject("data")

            for (key in dataObject.keys()) {
                val jsonItem = dataObject.getJSONObject(key)

                if (type == SelectListActivity.SELECT_LIST_TYPE_GRP) {
                    items.add(
                        SelectItem(
                            id = jsonItem.getInt("id"),
                            name = jsonItem.getString("txt"),
                            type = type
                        )
                    )
                } else {
                    items.add(
                        SelectItem(
                            id = jsonItem.getInt("id"),
                            agency = jsonItem.getInt("agency"),
                            group = jsonItem.getInt("group"),
                            ref = jsonItem.getString("ref"),
                            name = jsonItem.getString("name"),
                            entry = jsonItem.getString("entry"),
                            address = jsonItem.getJSONObject("adr").getString("rue"),
                            postalCode = jsonItem.getJSONObject("adr").getString("cp"),
                            city = jsonItem.getJSONObject("adr").getString("city"),
                            last = jsonItem.getString("last"),
                            delay = jsonItem.getBoolean("delay"),
                            comment = jsonItem.getString("comment"),
                            type = type
                        )
                    )
                }
            }
        } else {
            throw JSONException("Réponse serveur invalide")
        }

        return items
    }

    fun setUserData(userData: JSONObject) {
        this.userData = userData
    }

}
