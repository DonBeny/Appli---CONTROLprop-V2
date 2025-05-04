package org.orgaprop.controlprop.managers

import android.content.SharedPreferences

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.ObjConfig
import org.orgaprop.controlprop.models.ObjGrille
import org.orgaprop.controlprop.models.ObjGrilleElement
import org.orgaprop.controlprop.models.ObjGrilleZone
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils



class GrilleCtrlManager(
    private val sharedPrefs: SharedPreferences,
    private val syncManager: SyncManager,
    private val httpTask: HttpTask
) {

    private val TAG = "GrilleCtrlManager"

    private val gson = Gson()

    fun getGrilleElements(entry: SelectItem): Map<Int, List<ObjGrilleElement>> {
        try {
            val grille = entry.prop?.ctrl?.grille ?: return emptyMap()

            val elementsMap = mutableMapOf<Int, List<ObjGrilleElement>>()

            grille.zones.forEach { zone ->
                elementsMap[zone.zoneId] = zone.elements
            }

            return elementsMap
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error getting grille elements", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors de la récupération des éléments de la grille", e)
        }
    }



    fun updateGrilleData(entry: SelectItem, zoneId: Int, elements: List<ObjGrilleElement>): SelectItem {
        LogUtils.json(TAG, "updateGrilleData: entry:", entry)
        LogUtils.d(TAG, "updateGrilleData: zoneId: $zoneId")
        LogUtils.json(TAG, "updateGrilleData: elements:", elements)

        try {
            val currentGrille = entry.prop?.ctrl?.grille ?: ObjGrille()
            val zones = currentGrille.zones.toMutableList()

            LogUtils.json(TAG, "updateGrilleData: currentGrille:", currentGrille)
            LogUtils.json(TAG, "updateGrilleData: zones:", zones)

            val updatedZone = zones.find { it.zoneId == zoneId }?.copy(elements = elements)
                ?: ObjGrilleZone(zoneId = zoneId, elements = elements)

            LogUtils.json(TAG, "updateGrilleData: updatedZone:", updatedZone)

            val updatedZones = zones.map { if (it.zoneId == zoneId) updatedZone else it }
            if (!zones.any { it.zoneId == zoneId }) {
                zones.add(updatedZone)
            }

            LogUtils.json(TAG, "updateGrilleData: updatedZones:", updatedZones)

            val updatedGrille = ObjGrille(zones = updatedZones)

            LogUtils.json(TAG, "updateGrilleData: updatedGrille:", updatedGrille)

            val updatedEntry = entry.copy(
                prop = entry.prop?.copy(
                    ctrl = entry.prop.ctrl.copy(
                        grille = updatedGrille
                    )
                )
            )

            LogUtils.json(TAG, "updateGrilleData: updatedEntry:", updatedEntry)

            return updatedEntry
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error updating grille data", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la mise à jour des données de la grille", e)
        }
    }



    fun loadResidenceData(currentEntry: SelectItem, typeCtrl: String, confCtrl: JSONObject): SelectItem {
        LogUtils.json(TAG, "loadResidenceData: currentEntry:", currentEntry)
        LogUtils.d(TAG, "loadResidenceData: typeCtrl: $typeCtrl")
        LogUtils.json(TAG, "loadResidenceData: confCtrl:", confCtrl)

        val objConfig = try {
            ObjConfig(
                visite = confCtrl.optBoolean("visite", false),
                meteo = confCtrl.optBoolean("meteo", false),
                affichage = confCtrl.optBoolean("aff", true),
                produits = confCtrl.optBoolean("prod", true)
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error converting JSON to ObjConfig", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la récupération de la configuration", e)
        }

        LogUtils.json(TAG, "loadResidenceData: objConfig:", objConfig)

        try {
            val pendingsCtrls = syncManager.getPendingControls()

            LogUtils.json(TAG, "loadResidenceData: pendingsCtrls:", pendingsCtrls)

            val savedControl = pendingsCtrls
                .firstOrNull { it.id == currentEntry.id }
                ?.takeIf { it.prop?.ctrl?.grille != null && !it.signed && it.prop.ctrl.date.isToday() }

            if (savedControl != null) {
                val currentProp = currentEntry.prop
                val savedCtrl = savedControl.prop?.ctrl

                LogUtils.json(TAG, "loadResidenceData: savedControl:", savedControl)
                LogUtils.json(TAG, "loadResidenceData: currentProp:", currentProp)
                LogUtils.json(TAG, "loadResidenceData: savedCtrl:", savedCtrl)

                if (currentProp != null && savedCtrl != null) {
                    val selectedEntry = currentEntry.copy(
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

                    LogUtils.json(TAG, "loadResidenceData: Returning saved control", selectedEntry)

                    return selectedEntry
                } else {
                    LogUtils.e(TAG, "loadResidenceData: Invalid control data")
                }
            } else {
                LogUtils.d(TAG, "loadResidenceData: No saved control found")
            }

            LogUtils.json(TAG, "loadResidenceData: Returning current entry", currentEntry)

            val updatedEntry = currentEntry.copy(
                type = typeCtrl,
                prop = currentEntry.prop?.copy(
                    ctrl = currentEntry.prop.ctrl.copy(
                        conf = objConfig
                    )
                )
            )

            LogUtils.json(TAG, "loadResidenceData: Updated entry:", updatedEntry)

            saveControlProgress(updatedEntry)
            return updatedEntry
        } catch (e: BaseException) {
            LogUtils.e(TAG, "Error loading residence data", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading residence data", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors du chargement des données de la résidence", e)
        }
    }



    fun saveControlProgress(control: SelectItem) {
        LogUtils.json(TAG, "saveControlProgress: control:", control)

        try {
            syncManager.addOrUpdatePendingControl(control)

            LogUtils.d(TAG, "saveControlProgress: Control saved successfully")
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error saving control progress", e)
            throw BaseException(ErrorCodes.SYNC_FAILED, "Erreur lors de la sauvegarde du contrôle", e)
        }
    }



    fun finishCtrl(callback: (Boolean) -> Unit) {
        LogUtils.d(TAG, "finishCtrl: Starting synchronization")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = syncManager.syncPendingControls()

                LogUtils.json(TAG, "finishCtrl: Sync result:", result)

                withContext(Dispatchers.Main) {
                    when (result) {
                        is SyncManager.SyncResult.SUCCESS -> {
                            LogUtils.d(TAG, "finishCtrl: Synchronization successful")
                            callback(true)
                        }
                        is SyncManager.SyncResult.PARTIAL_SUCCESS -> {
                            LogUtils.d(TAG, "finishCtrl: Synchronization partially successful")
                            callback(true) // Considérer un succès partiel comme un succès
                        }
                        is SyncManager.SyncResult.FAILURE -> {
                            LogUtils.e(TAG, "finishCtrl: Synchronization failed")
                            callback(false)
                        }
                        is SyncManager.SyncResult.NO_NETWORK -> {
                            LogUtils.e(TAG, "finishCtrl: No network available")
                            callback(false)
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error during synchronization", e)

                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

}
