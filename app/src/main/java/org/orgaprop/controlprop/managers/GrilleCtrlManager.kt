package org.orgaprop.controlprop.managers

import android.content.SharedPreferences
import android.util.Log

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.ObjConfig
import org.orgaprop.controlprop.models.ObjDateCtrl

import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.utils.HttpTask

class GrilleCtrlManager(
    private val sharedPrefs: SharedPreferences,
    private val syncManager: SyncManager,
    private val httpTask: HttpTask
) {

    private val TAG = "GrilleCtrlManager"

    private val gson = Gson()

    private fun getPendingControls(): List<SelectItem> {
        Log.d(TAG, "getPendingControls: Getting pending controls")
        Log.d(TAG, "Accessing SharedPrefs: ${sharedPrefs.javaClass.name}")
        Log.d(TAG, "All keys in SharedPrefs: ${sharedPrefs.all.keys}")

        try {
            val json = sharedPrefs.getString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, null)

            Log.d(TAG, "getPendingControls: json: $json")

            return json?.let {
                val type = object : TypeToken<List<SelectItem>>() {}.type
                gson.fromJson(it, type) ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pending controls", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Impossible de charger les contrôles en attente", e)
        }
    }

    fun getGrilleElements(entry: SelectItem): Map<Int, List<ObjElement>> {
        try {
            val grille = entry.prop?.ctrl?.grille ?: return emptyMap()
            if (grille == "[]") return emptyMap()

            val jsonArray = JSONArray(grille)
            val elementsMap = mutableMapOf<Int, List<ObjElement>>()

            for (i in 0 until jsonArray.length()) {
                val zoneObj = jsonArray.getJSONObject(i)
                val zoneId = zoneObj.getInt("zoneId")
                val elements = gson.fromJson<List<ObjElement>>(
                    zoneObj.getJSONArray("elements").toString(),
                    object : TypeToken<List<ObjElement>>() {}.type
                )
                elementsMap[zoneId] = elements
            }

            return elementsMap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting grille elements", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors de la récupération des éléments de la grille", e)
        }
    }



    fun updateGrilleData(entry: SelectItem, zoneId: Int, elements: List<ObjElement>): SelectItem {
        try {
            val currentGrille = if (entry.prop?.ctrl?.grille.isNullOrEmpty()) {
                JSONArray()
            } else {
                JSONArray(entry.prop!!.ctrl.grille)
            }

            Log.d(TAG, "updateGrilleData: currentGrille: $currentGrille")

            val updatedZone = JSONObject().apply {
                put("zoneId", zoneId)
                put("elements", JSONArray(gson.toJson(elements)))
                put("timestamp", System.currentTimeMillis())
            }

            Log.d(TAG, "updateGrilleData: updatedZone: $updatedZone")

            var found = false
            for (i in 0 until currentGrille.length()) {
                Log.d(TAG, "updateGrilleData: currentGrille[$i]: ${currentGrille.getJSONObject(i)}")

                if (currentGrille.getJSONObject(i).getInt("zoneId") == zoneId) {
                    Log.d(TAG, "updateGrilleData: Updating zone $zoneId")

                    currentGrille.put(i, updatedZone)
                    found = true
                    break
                }
            }
            if (!found) {
                currentGrille.put(updatedZone)
            }

            Log.d(TAG, "updateGrilleData: currentGrille: $currentGrille")

            return entry.copy(
                prop = entry.prop?.copy(
                    ctrl = entry.prop.ctrl.copy(
                        grille = currentGrille.toString()
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating grille data", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la mise à jour des données de la grille", e)
        }
    }



    fun loadResidenceData(currentEntry: SelectItem, typeCtrl: String, confCtrl: JSONObject): SelectItem {
        Log.d(TAG, "loadResidenceData: currentEntry: $currentEntry")
        Log.d(TAG, "loadResidenceData: typeCtrl: $typeCtrl")
        Log.d(TAG, "loadResidenceData: confCtrl: $confCtrl")

        val objConfig = try {
            ObjConfig(
                visite = confCtrl.optBoolean("visite", false),
                meteo = confCtrl.optBoolean("meteo", false),
                affichage = confCtrl.optBoolean("aff", true),
                produits = confCtrl.optBoolean("prod", true)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting JSON to ObjConfig", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de la configuration", e)
        }

        try {
            val savedControl = getPendingControls()
                .firstOrNull { it.id == currentEntry.id }
                ?.takeIf { it.prop?.ctrl?.grille != "[]" }

            if (savedControl != null) {
                val currentProp = currentEntry.prop
                val savedCtrl = savedControl.prop?.ctrl

                Log.d(TAG, "loadResidenceData: savedControl: ${savedControl.id} => ${savedControl.prop?.ctrl?.date?.value} => ${savedControl.prop?.ctrl?.note}")
                Log.d(TAG, "loadResidenceData: currentProp: ${currentProp?.ctrl?.date?.value} => ${currentProp?.ctrl?.note}")
                Log.d(TAG, "loadResidenceData: savedCtrl: ${savedCtrl?.date?.value} => ${savedCtrl?.note}")

                if (currentProp != null && savedCtrl != null) {
                    return currentEntry.copy(
                        type = typeCtrl,
                        prop = currentProp.copy(
                            ctrl = currentProp.ctrl.copy(
                                note = savedCtrl.note,
                                conf = objConfig,
                                date = savedCtrl.date,
                                prestate = savedCtrl.prestate,
                                grille = savedCtrl.grille
                            )
                        )
                    )
                }
            }

            val updatedEntry = currentEntry.copy(
                type = typeCtrl,
                prop = currentEntry.prop?.copy(
                    ctrl = currentEntry.prop.ctrl.copy(
                        conf = objConfig
                    )
                )
            )

            saveControlProgress(updatedEntry)
            return updatedEntry
        } catch (e: BaseException) {
            Log.e(TAG, "Error loading residence data", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error loading residence data", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors du chargement des données de la résidence", e)
        }
    }



    fun saveControlProgress(control: SelectItem) {
        try {
            val current = getPendingControls().toMutableList()

            Log.d(TAG, "saveControlProgress: current: $current")

            val existingIndex = current.indexOfFirst { it.id == control.id }
            if (existingIndex != -1) {
                Log.d(TAG, "saveControlProgress: Replacing existing control for ID ${control.id}")
                current.removeAt(existingIndex)
            } else {
                Log.d(TAG, "saveControlProgress: Adding new control for ID ${control.id}")
            }

            current.add(control)

            sharedPrefs.edit()
                .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(current))
                .apply()

            Log.d(TAG, "saveControlProgress: Control saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving control progress", e)
            throw BaseException(ErrorCodes.SYNC_FAILED, "Erreur lors de la sauvegarde de la progression du contrôle", e)
        }
    }



    fun finishCtrl(callback: (Boolean) -> Unit) {
        Log.d(TAG, "finishCtrl: Starting synchronization")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = syncManager.syncPendingControls()
                Log.d(TAG, "finishCtrl: Sync result: $result")

                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during synchronization", e)

                withContext(Dispatchers.Main) {
                    // Même en cas d'erreur, on notifie le callback pour éviter de bloquer l'UI
                    callback(false)
                }
            }
        }
    }

}
