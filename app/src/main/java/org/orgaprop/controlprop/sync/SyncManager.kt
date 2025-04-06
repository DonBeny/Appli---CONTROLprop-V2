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
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.utils.HttpTask
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
        Log.d(TAG, "syncPendingControls: Checking network")
        Log.d(TAG, "syncPendingControls: Network available: ${networkMonitor.isNetworkAvailable.value}")

        if (!networkMonitor.isNetworkAvailable.value) {
            return@withContext SyncResult.NO_NETWORK
        }

        try {
            val pendingControls = getPendingControls()
            val userData = getUserData()

            Log.d(TAG, "syncPendingControls: pendingControls: $pendingControls")
            Log.d(TAG, "syncPendingControls: userData: $userData")

            if (pendingControls.isEmpty()) {
                return@withContext SyncResult.SUCCESS
            }
            if (userData == null) {
                return@withContext SyncResult.FAILURE
            }

            val controlsJson = gson.toJson(pendingControls)
            val paramsGet = ""
            val paramsPost = "data=$controlsJson&mbr=${userData.first}&mac=${userData.second}"

            Log.d(TAG, "syncPendingControls: paramsPost: $paramsPost")

            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_PROP,
                HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_SAVE,
                paramsGet,
                paramsPost
            )

            Log.d(TAG, "syncPendingControls: response: $response")

            val syncResponse = gson.fromJson(response, SyncResponse::class.java)
            val status = syncResponse.status
            val data = syncResponse.data

            Log.d(TAG, "syncPendingControls: status: $status")
            Log.d(TAG, "syncPendingControls: data: $data")

            if (status) {
                val savedIds = data.saved
                val errors = data.error

                Log.d(TAG, "syncPendingControls: savedIds: $savedIds")
                Log.d(TAG, "syncPendingControls: errors: $errors")

                updateSyncedControls(savedIds)
                cleanSyncedControls(savedIds)

                return@withContext if (errors.isEmpty()) {
                    SyncResult.SUCCESS
                } else {
                    logSyncErrors(errors)
                    SyncResult.PARTIAL_SUCCESS
                }
            } else {
                Log.e(TAG, "Sync failed with response: $response")
                return@withContext SyncResult.FAILURE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.FAILURE
        }
    }

    private fun getPendingControls(): List<SelectItem> {
        return try {
            Log.d(TAG, "Accessing SharedPrefs: ${sharedPrefs.javaClass.name}")
            Log.d(TAG, "All keys in SharedPrefs: ${sharedPrefs.all.keys}")

            val json = sharedPrefs.getString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, null)

            Log.d(TAG, "getPendingControls: json: $json")

            json?.let {
                val type = object : TypeToken<List<SelectItem>>() {}.type
                gson.fromJson(it, type) ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pending controls", e)
            emptyList()
        }
    }
    private fun getUserData(): Pair<String, String>? {
        return try {
            val userData = sharedPrefs.getString(PREF_SAVED_USER, null)

            if( userData != null ) {
                val userDataJson = gson.fromJson(userData, LoginData::class.java)

                return Pair(userDataJson.idMbr.toString(), userDataJson.adrMac)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user data", e)
            null
        }
    }

    private fun updateSyncedControls(savedItems: List<SavedItem>) {
        Log.d(TAG, "updateSyncedControls: savedItems: $savedItems")

        val currentControls = getPendingControls().toMutableList()

        Log.d(TAG, "updateSyncedControls: currentControls: $currentControls")

        val updatedControls = currentControls.map { control ->
            Log.d(TAG, "updateSyncedControls: control: $control")

            savedItems.find { it.id == control.id }?.let { savedItem ->
                Log.d(TAG, "updateSyncedControls: savedItem: $savedItem")

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

        Log.d(TAG, "updateSyncedControls: updatedControls: $updatedControls")

        sharedPrefs.edit()
            .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(updatedControls))
            .apply()
    }
    private fun cleanSyncedControls(savedIds: List<SavedItem>) {
        Log.d(TAG, "cleanSyncedControls: savedIds: $savedIds")

        val currentControls = getPendingControls()

        Log.d(TAG, "cleanSyncedControls: currentControls: $currentControls")

        val updatedControls = currentControls.filter { control ->
            Log.d(TAG, "cleanSyncedControls: control: $control")

            val savedItem = savedIds.find { it.id == control.id }

            Log.d(TAG, "cleanSyncedControls: savedItem: $savedItem")

            if (savedItem != null) {
                // Garder seulement si non signé ET date est aujourd'hui
                !control.signed && ObjDateCtrl(savedItem.date.value).isToday()
            } else {
                true
            }
        }

        Log.d(TAG, "cleanSyncedControls: updatedControls: $updatedControls")

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
            Log.e(TAG, "Sync error for control $id: $message")
            // Ici vous pourriez aussi notifier l'utilisateur via un Snackbar ou Toast
        }
    }



    sealed class SyncResult {
        data object SUCCESS : SyncResult()
        data object FAILURE : SyncResult()
        data object NO_NETWORK : SyncResult()
        data object PARTIAL_SUCCESS : SyncResult()
    }

}
