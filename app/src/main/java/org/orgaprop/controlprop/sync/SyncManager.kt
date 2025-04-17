package org.orgaprop.controlprop.sync

import android.content.SharedPreferences
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.json.JSONArray

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
        val saved: List<SavedItem>,
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

            LogUtils.json(TAG, "syncPendingControls: pendingControls:", pendingControls)
            LogUtils.json(TAG, "syncPendingControls: userData:", userData)

            if (pendingControls.isEmpty()) {
                return@withContext SyncResult.SUCCESS
            }
            if (userData == null) {
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

            val syncResponse = gson.fromJson(response, SyncResponse::class.java)
            val status = syncResponse.status
            val data = syncResponse.data

            LogUtils.d(TAG, "syncPendingControls: status: $status")
            LogUtils.json(TAG, "syncPendingControls: data:", data)

            if (status) {
                val savedIds = data.saved
                val errors = data.error

                LogUtils.json(TAG, "syncPendingControls: savedIds:", savedIds)
                LogUtils.json(TAG, "syncPendingControls: errors:", errors)

                updateSyncedControls(savedIds)
                cleanSyncedControls(savedIds)

                return@withContext if (errors.isEmpty()) {
                    SyncResult.SUCCESS
                } else {
                    logSyncErrors(errors)
                    SyncResult.PARTIAL_SUCCESS(errors)
                }
            } else {
                LogUtils.json(TAG, "Sync failed with response:", response)
                return@withContext SyncResult.FAILURE
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Sync failed", e)
            SyncResult.FAILURE
        }
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

    private fun updateSyncedControls(savedItems: List<SavedItem>) {
        LogUtils.json(TAG, "updateSyncedControls: savedItems:", savedItems)

        val currentControls = getPendingControls().toMutableList()

        LogUtils.json(TAG, "updateSyncedControls: currentControls:", currentControls)

        val updatedControls = currentControls.map { control ->
            LogUtils.json(TAG, "updateSyncedControls: control:", control)

            savedItems.find { it.id == control.id }?.let { savedItem ->
                LogUtils.json(TAG, "updateSyncedControls: savedItem:", savedItem)

                control.copy(
                    prop = control.prop?.copy(
                        ctrl = control.prop.ctrl.copy(
                            date = ObjDateCtrl(savedItem.date.value, savedItem.date.txt)
                        )
                    ),
                    saved = true
                )
            } ?: control
        }

        LogUtils.json(TAG, "updateSyncedControls: updatedControls:", updatedControls)

        sharedPrefs.edit()
            .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(updatedControls))
            .apply()
    }
    private fun cleanSyncedControls(savedIds: List<SavedItem>) {
        LogUtils.json(TAG, "cleanSyncedControls: savedIds:", savedIds)

        val currentControls = getPendingControls()

        LogUtils.json(TAG, "cleanSyncedControls: currentControls:", currentControls)

        val updatedControls = currentControls.filter { control ->
            LogUtils.json(TAG, "cleanSyncedControls: control:", control)

            val savedItem = savedIds.find { it.id == control.id }

            LogUtils.json(TAG, "cleanSyncedControls: savedItem:", savedItem)

            if (savedItem != null) {
                !control.signed && ObjDateCtrl(savedItem.date.value * 1000).isToday()
            } else {
                true
            }
        }

        LogUtils.json(TAG, "cleanSyncedControls: updatedControls:", updatedControls)

        sharedPrefs.edit()
            .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(updatedControls))
            .apply()
    }



    private fun parseIdList(jsonArray: JSONArray?): List<Int> {
        return if (jsonArray != null) {
            (0 until jsonArray.length()).mapNotNull { i ->
                jsonArray.optInt(i, -1).takeIf { it != -1 }
            }
        } else {
            emptyList()
        }
    }
    private fun parseErrorList(jsonArray: JSONArray?): List<Pair<Int, String>> {
        return if (jsonArray != null) {
            (0 until jsonArray.length()).mapNotNull { i ->
                val errorObj = jsonArray.optJSONObject(i)
                if (errorObj != null) {
                    val id = errorObj.optInt("id", -1)
                    val txt = errorObj.optString("txt", "")
                    if (id != -1) id to txt else null
                } else {
                    null
                }
            }
        } else {
            emptyList()
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
