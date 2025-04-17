package org.orgaprop.controlprop.managers

import android.content.SharedPreferences
import android.util.Log

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

import org.orgaprop.controlprop.models.ObjSignature
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.utils.LogUtils

class SignatureManager(
    private val sharedPrefs: SharedPreferences,
    private val syncManager: SyncManager
) {

    companion object {
        private const val TAG = "SignatureManager"
    }

    private val gson = Gson()



    suspend fun saveSignatures(entry: SelectItem, signatureData: ObjSignature): SignatureResult {
        return try {
            LogUtils.json(TAG, "saveSignatures: Début de la sauvegarde des signatures pour l'entrée", entry)

            validateSignatureData(signatureData)

            val updatedEntry = entry.copy(
                signed = true,
                signatures = signatureData,
            )

            updateSharedPreferences(updatedEntry)

            LogUtils.d(TAG, "saveSignatures: Entrée mise à jour dans les préférences")

            syncManager.addOrUpdatePendingControl(updatedEntry)

            LogUtils.d(TAG, "saveSignatures: SyncManager pendingControls mis à jour")

            val syncResult = syncManager.syncPendingControls()
            LogUtils.json(TAG, "saveSignatures: Résultat de la synchronisation:", syncResult)

            handleSyncResult(updatedEntry, syncResult)
        } catch (e: BaseException) {
            LogUtils.e(TAG, "saveSignatures: BaseException lors de la sauvegarde", e)
            SignatureResult.Error(e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            LogUtils.e(TAG, "saveSignatures: Exception lors de la sauvegarde", e)
            SignatureResult.Error("Erreur lors de la sauvegarde des signatures: ${e.message}")
        }
    }



    private fun validateSignatureData(signatureData: ObjSignature) {
        if (signatureData.controlSignature.isBlank() && signatureData.agentSignature.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_INPUT, "Aucune signature fournie")
        }
    }



    private fun updateSharedPreferences(updatedEntry: SelectItem) {
        try {
            LogUtils.json(TAG, "updateSharedPreferences: Mise à jour de l'entrée dans les préférences:", updatedEntry)

            // Mettre à jour l'entrée sélectionnée
            sharedPrefs.edit()
                .putString(BaseActivity.PREF_SAVED_ENTRY_SELECTED, gson.toJson(updatedEntry))
                .apply()
            LogUtils.d(TAG, "updateSharedPreferences: Entrée sélectionnée mise à jour")

            // Mettre à jour la liste des contrôles en attente
            val pendingControls = getPendingControls().map {
                if (it.id == updatedEntry.id) updatedEntry else it
            }
            sharedPrefs.edit()
                .putString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, gson.toJson(pendingControls))
                .apply()
            LogUtils.json(TAG, "updateSharedPreferences: Liste des contrôles en attente mise à jour, nombre d'éléments: ${pendingControls.size}", pendingControls)
        } catch (e: Exception) {
            LogUtils.e(TAG, "updateSharedPreferences: Erreur lors de la mise à jour des préférences", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la mise à jour des préférences", e)
        }
    }
    private fun handleSyncResult(updatedEntry: SelectItem, syncResult: SyncManager.SyncResult): SignatureResult {
        return when (syncResult) {
            is SyncManager.SyncResult.SUCCESS -> {
                LogUtils.d(TAG, "handleSyncResult: Synchronisation réussie")
                SignatureResult.Success(updatedEntry)
            }
            is SyncManager.SyncResult.PARTIAL_SUCCESS -> {
                val failedIds = syncResult.errors.map { it.id }
                LogUtils.json(TAG, "handleSyncResult: Synchronisation partiellement réussie, éléments échoués:", failedIds)

                if (failedIds.contains(updatedEntry.id)) {
                    LogUtils.e(TAG, "handleSyncResult: L'élément courant fait partie des échecs")
                }

                SignatureResult.PartialSuccess(updatedEntry, failedIds)
            }
            is SyncManager.SyncResult.NO_NETWORK -> {
                LogUtils.e(TAG, "handleSyncResult: Pas de réseau disponible")
                SignatureResult.Error("Signatures enregistrées localement. Synchronisation impossible: pas de connexion réseau")
            }
            else -> {
                LogUtils.e(TAG, "handleSyncResult: Échec de la synchronisation")
                SignatureResult.Error("Échec de la synchronisation avec le serveur")
            }
        }
    }

    private fun getPendingControls(): List<SelectItem> {
        LogUtils.d(TAG, "getPendingControls: Getting pending controls")

        return try {
            val json = sharedPrefs.getString(BaseActivity.PREF_SAVED_PENDING_CONTROLS, null)
            LogUtils.json(TAG, "getPendingControls: json récupéré:", json)

            json?.let {
                val type = object : TypeToken<List<SelectItem>>() {}.type
                val controls = gson.fromJson<List<SelectItem>>(it, type) ?: emptyList()
                LogUtils.json(TAG, "getPendingControls: ${controls.size} contrôles récupérés", controls)
                controls
            } ?: run {
                LogUtils.d(TAG, "getPendingControls: Aucun contrôle en attente trouvé")
                emptyList()
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading pending controls", e)
            emptyList()
        }
    }



    sealed class SignatureResult {
        data class Success(val updatedEntry: SelectItem) : SignatureResult()
        data class PartialSuccess(val updatedEntry: SelectItem, val failedIds: List<Int>) : SignatureResult()
        data class Error(val message: String) : SignatureResult()
    }

}
