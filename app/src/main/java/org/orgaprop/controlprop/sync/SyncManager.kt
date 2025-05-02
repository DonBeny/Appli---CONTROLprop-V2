package org.orgaprop.controlprop.sync

import android.content.SharedPreferences
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.json.JSONArray
import org.json.JSONObject

import org.orgaprop.controlprop.models.ObjDateCtrl
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.BaseActivity.Companion.PREF_SAVED_USER
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class SyncManager(
    private val networkMonitor: NetworkMonitor,
    private val httpTask: HttpTask,
    private val sharedPrefs: SharedPreferences
) {

    private val TAG = "SyncManager"

    private val gson = Gson()

    data class SyncResponse(
        val status: Boolean,
        val data: SyncData
    )
    data class SyncData(
        val saved: List<SelectItem>,
        val error: List<ErrorItem>
    )
    data class SavedItem(
        val id: Int,
        val date: ReturnedDate
    )
    data class ReturnedDate(
        val value: Long,
        val txt: String
    )
    data class ErrorItem(
        val id: Int,
        val txt: String
    )



    suspend fun syncPendingControls(): SyncResult = withContext(Dispatchers.IO) {
        LogUtils.d(TAG, "syncPendingControls: Checking network")
        LogUtils.d(TAG, "syncPendingControls: Network available: ${networkMonitor.isNetworkAvailable.value}")

        if (!networkMonitor.isNetworkAvailable.value) {
            return@withContext SyncResult.NO_NETWORK
        }

        try {
            val pendingControls = getPendingControls()
            val userData = getUserData()

            if (pendingControls.isEmpty()) {
                LogUtils.d(TAG, "syncPendingControls: No pending controls to sync")

                return@withContext SyncResult.SUCCESS
            }
            if (userData == null) {
                LogUtils.e(TAG, "syncPendingControls: User data is null")

                return@withContext SyncResult.FAILURE
            }

            val controlsJson = gson.toJson(pendingControls)
            val paramsGet = ""
            val paramsPost = "data=$controlsJson&mbr=${userData.first}&mac=${userData.second}"

            LogUtils.json(TAG, "syncPendingControls: paramsPost:", paramsPost)

            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_PROP,
                HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_SAVE,
                paramsGet,
                paramsPost
            )

            LogUtils.json(TAG, "syncPendingControls: response:", response)

            try {
                val jsonObject = JSONObject(response)
                val status = jsonObject.getBoolean("status")

                LogUtils.json(TAG, "syncPendingControls: jsonObject:", jsonObject)

                if (status) {
                    val dataObj = jsonObject.getJSONObject("data")
                    val savedArray = dataObj.getJSONArray("saved")

                    for (i in 0 until savedArray.length()) {
                        val itemObj = savedArray.getJSONObject(i)

                        updatePendingControlFromServerResponse(itemObj)
                    }

                    val errorArray = dataObj.getJSONArray("error")
                    val errorItems = mutableListOf<ErrorItem>()

                    for (i in 0 until errorArray.length()) {
                        val errorObj = errorArray.optJSONObject(i)
                        if (errorObj != null) {
                            errorItems.add(
                                ErrorItem(
                                    errorObj.getInt("id"),
                                    errorObj.getString("txt")
                                )
                            )
                        }
                    }

                    cleanSyncedControls()

                    return@withContext if (errorItems.isEmpty()) {
                        SyncResult.SUCCESS
                    } else {
                        logSyncErrors(errorItems)
                        SyncResult.PARTIAL_SUCCESS(errorItems)
                    }
                } else {
                    LogUtils.json(TAG, "Sync failed with response:", response)
                    return@withContext SyncResult.FAILURE
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error parsing response", e)
                return@withContext SyncResult.FAILURE
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Sync failed", e)
            return@withContext SyncResult.FAILURE
        }
    }

    /**
     * Met à jour un contrôle en attente avec les données reçues du serveur.
     * Cette méthode est appelée pour chaque élément reçu dans la réponse du serveur.
     *
     * @param serverItem L'élément JSON reçu du serveur
     */
    private fun updatePendingControlFromServerResponse(serverItem: JSONObject) {
        try {
            val id = serverItem.getInt("id")

            val type = serverItem.optString("type", "")
            val selectItem = SelectItem.fromResidenceJson(serverItem, type)

            addOrUpdatePendingControl(selectItem)

            val entrySelected = sharedPrefs.getString(BaseActivity.PREF_SAVED_ENTRY_SELECTED, null)
            if (entrySelected != null) {
                val entry = gson.fromJson(entrySelected, SelectItem::class.java)
                if (entry.id == id) {
                    LogUtils.d(TAG, "Mise à jour du contrôle sélectionné dans BaseActivity")
                    sharedPrefs.edit()
                        .putString(BaseActivity.PREF_SAVED_ENTRY_SELECTED, gson.toJson(selectItem))
                        .apply()
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Erreur lors de la mise à jour du contrôle depuis la réponse du serveur", e)
        }
    }
    private fun cleanSyncedControls() {
        val currentControls = getPendingControls()

        LogUtils.json(TAG, "cleanSyncedControls: currentControls:", currentControls)

        val updatedControls = currentControls.filter { control ->
            LogUtils.json(TAG, "cleanSyncedControls: control:", control)

            val isNotSigned = !control.signed
            val isToday = control.prop?.ctrl?.date?.isToday() ?: false
            val isNotSaved = !control.saved

            val keepControl = isNotSigned && isToday && isNotSaved

            if (keepControl) {
                LogUtils.d(TAG, "Le contrôle ID: ${control.id} est conservé (non signé ET du jour)")
            } else {
                LogUtils.d(TAG, "Le contrôle ID: ${control.id} est supprimé (signé=${control.signed}, isToday=$isToday, isNotSaved=$isNotSaved)")
            }

            keepControl
        }

        LogUtils.json(TAG, "cleanSyncedControls: updatedControls:", updatedControls)

        sharedPrefs.edit()
            .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(updatedControls))
            .apply()
    }



    private fun getUserData(): Pair<String, String>? {
        return try {
            val userData = sharedPrefs.getString(PREF_SAVED_USER, null)

            if( userData != null ) {
                val userDataJson = gson.fromJson(userData, LoginData::class.java)

                //LogUtils.json(TAG, "getUserData: userDataJson:", userDataJson)

                return Pair(userDataJson.idMbr.toString(), userDataJson.adrMac)
            } else {
                LogUtils.e(TAG, "getUserData: userData is null")
                null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading user data", e)
            null
        }
    }



    private fun logSyncErrors(errors: List<ErrorItem>) {
        errors.forEach { (id, message) ->
            LogUtils.e(TAG, "Sync error for control $id: $message")
        }
    }



    fun getPendingControls(): List<SelectItem> {
        return try {
            LogUtils.d(TAG, "Accessing SharedPrefs: ${sharedPrefs.javaClass.name}")
            LogUtils.d(TAG, "All keys in SharedPrefs: ${sharedPrefs.all.keys}")

            val json = sharedPrefs.getString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, null)

            LogUtils.json(TAG, "getPendingControls: json:", json)

            json?.let {
                val type = object : TypeToken<List<SelectItem>>() {}.type
                gson.fromJson(it, type) ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading pending controls", e)
            emptyList()
        }
    }

    fun savePendingControls(controls: List<SelectItem>) {
        sharedPrefs.edit()
            .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(controls))
            .apply()
        LogUtils.json(TAG, "savePendingControls: ${controls.size} controls saved", controls)
    }

    /**
     * Ajoute ou met à jour un contrôle dans la liste des contrôles en attente.
     * Cette méthode peut être utilisée par les différentes activities pour maintenir
     * à jour la liste des contrôles à synchroniser.
     *
     * @param control Le contrôle à ajouter ou mettre à jour
     * @return true si l'opération est réussie, false sinon
     */
    fun addOrUpdatePendingControl(control: SelectItem): Boolean {
        return try {
            LogUtils.json(TAG, "addOrUpdatePendingControl: Ajout/mise à jour du contrôle:", control)

            val currentControls = getPendingControls().toMutableList()

            val existingIndex = currentControls.indexOfFirst { it.id == control.id }

            if (existingIndex >= 0) {
                LogUtils.d(TAG, "addOrUpdatePendingControl: Mise à jour d'un contrôle existant")
                currentControls[existingIndex] = control
            } else {
                LogUtils.d(TAG, "addOrUpdatePendingControl: Ajout d'un nouveau contrôle")
                currentControls.add(control)
            }

            savePendingControls(currentControls)

            LogUtils.json(TAG, "addOrUpdatePendingControl: Contrôle ajouté/mis à jour avec succès:", control)
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "addOrUpdatePendingControl: Erreur lors de l'ajout/mise à jour du contrôle", e)
            false
        }
    }



    sealed class SyncResult {
        data object SUCCESS : SyncResult()
        data object FAILURE : SyncResult()
        data object NO_NETWORK : SyncResult()
        data class PARTIAL_SUCCESS(val errors: List<ErrorItem>) : SyncResult()
    }

}
