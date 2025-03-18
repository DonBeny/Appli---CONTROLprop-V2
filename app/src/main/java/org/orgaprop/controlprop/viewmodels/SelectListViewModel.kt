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
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity

class SelectListViewModel(
    private val selectListManager: SelectListManager
): ViewModel() {

    private val TAG = "SelectListViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private val _items = MutableLiveData<List<SelectItem>>()
    val items: LiveData<List<SelectItem>> get() = _items

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private lateinit var userData: LoginData

    fun fetchData(type: String, parentId: Int, searchQuery: String) {
        viewModelScope.launch {
            try {
                val items = when (type) {
                    SelectListActivity.SELECT_LIST_TYPE_AGC -> fetchAgencesFromUserData()
                    else -> {
                        val jsonResponse = selectListManager.fetchData(
                            type,
                            parentId,
                            searchQuery,
                            userData.idMbr,
                            userData.adrMac
                        )
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
        return userData.let {
            val agencesArray = it.agences
            val agences = mutableListOf<SelectItem>()
            for (i in agencesArray.indices) {
                val agence = agencesArray.get(i)

                agences.add(
                    SelectItem(
                        id = agence.id,
                        name = agence.nom,
                        type = SelectListActivity.SELECT_LIST_TYPE_AGC
                    )
                )
            }
            agences
        }
    }

    private fun parseJsonResponse(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val items = mutableListOf<SelectItem>()

        Log.d(TAG, "parseJsonResponse: $jsonResponse")
        Log.d(TAG, "parseJsonResponse: type=$type")

        if (jsonResponse.getBoolean("status")) {
            val dataArray = jsonResponse.getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val jsonItem = dataArray.getJSONObject(i)

                Log.d(TAG, "parseJsonResponse: jsonItem=$jsonItem")

                val item = if (type == SelectListActivity.SELECT_LIST_TYPE_GRP) {
                    SelectItem(
                        id = jsonItem.getInt("id"),
                        name = jsonItem.getString("txt"),
                        type = type
                    )
                } else {
                    SelectItem(
                        id = jsonItem.getInt("id"),
                        ref = jsonItem.getString("ref"),
                        name = jsonItem.getString("name"),
                        entry = jsonItem.getString("entry"),
                        agency = jsonItem.getInt("agency"),
                        group = jsonItem.getInt("group"),
                        address = jsonItem.getJSONObject("adr").getString("rue"),
                        postalCode = jsonItem.getJSONObject("adr").getString("cp"),
                        city = jsonItem.getJSONObject("adr").getString("city"),
                        last = jsonItem.getString("last"),
                        delay = jsonItem.getBoolean("delay"),
                        comment = jsonItem.getString("comment"),
                        type = type
                    )
                }

                Log.d(TAG, "parseJsonResponse: item=$item")

                items.add(item)
            }

            Log.d(TAG, "parseJsonResponse: items=$items")
        } else {
            throw JSONException("Données non valides")
        }

        return items
    }

    fun setUserData(userData: LoginData) {
        this.userData = userData
    }

}
