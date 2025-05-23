package org.orgaprop.controlprop.managers

import android.content.SharedPreferences

import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.utils.LogUtils

class FinishCtrlManager(
    private val sharedPrefs: SharedPreferences,
    private val syncManager: SyncManager
) {

    companion object {
        const val TAG = "FinishCtrlManager"
    }



    /**
     * Vérifie si un contrôle est signé
     *
     * @param controlId L'ID du contrôle à vérifier
     * @return true si le contrôle est signé, false sinon
     */
    fun isControlSigned(controlId: Int): Boolean {
        val pendingControls = syncManager.getPendingControls()
        val control = pendingControls.find { it.id == controlId }
        val isSigned = control?.signed ?: false

        LogUtils.d(TAG, "isControlSigned: ID=$controlId, isSigned=$isSigned")
        return isSigned
    }

    /**
     * Vérifie si un contrôle a un plan d'actions
     *
     * @param controlId L'ID du contrôle à vérifier
     * @return true si le contrôle a un plan d'actions, false sinon
     */
    fun hasControlPlanActions(controlId: Int): Boolean {
        val pendingControls = syncManager.getPendingControls()
        val control = pendingControls.find { it.id == controlId }
        val hasPlanActions = control?.planActions != null

        LogUtils.d(TAG, "hasControlPlanActions: ID=$controlId, hasPlanActions=$hasPlanActions")
        return hasPlanActions
    }

}
