package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import org.orgaprop.controlprop.managers.FinishCtrlManager
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.utils.LogUtils

class FinishCtrlViewModel(private val manager: FinishCtrlManager) : ViewModel() {

    companion object {
        private const val TAG = "FinishCtrlViewModel"
    }

    private val _controlState = MutableLiveData<ControlState>()
    val controlState: LiveData<ControlState> = _controlState

    private lateinit var userData: LoginData
    private lateinit var entrySelected: SelectItem

    init {
        _controlState.value = ControlState()
    }



    /**
     * Initialise les données de l'utilisateur
     *
     * @param userData Les données de l'utilisateur
     */
    fun setUserData(userData: LoginData) {
        this.userData = userData
        //LogUtils.json(TAG, "setUserData:", userData)
    }

    /**
     * Initialise l'entrée sélectionnée et met à jour l'état du contrôle
     *
     * @param currentEntry L'entrée sélectionnée
     */
    fun setEntrySelected(currentEntry: SelectItem) {
        entrySelected = currentEntry
        //LogUtils.json(TAG, "setEntrySelected:", currentEntry)
        refreshControlState()
    }



    fun refreshControlState() {
        if (::entrySelected.isInitialized) {
            try {
                val isSigned = manager.isControlSigned(entrySelected.id)
                val hasPlanActions = manager.hasControlPlanActions(entrySelected.id)
                val isRandomControl = entrySelected.type == TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_RANDOM

                _controlState.value = ControlState(
                    isSigned = isSigned,
                    hasPlanActions = hasPlanActions,
                    isRandomControl = isRandomControl
                )

                LogUtils.d(TAG, "refreshControlState: isSigned=$isSigned, hasPlanActions=$hasPlanActions, isRandomControl=$isRandomControl")
            } catch (e: Exception) {
                LogUtils.e(TAG, "refreshControlState: Erreur lors du rafraîchissement de l'état", e)
            }
        } else {
            LogUtils.e(TAG, "refreshControlState: Aucune entrée sélectionnée")
        }
    }



    /**
     * État du contrôle
     *
     * @property isSigned Indique si le contrôle est signé
     * @property hasPlanActions Indique si le contrôle a un plan d'actions
     * @property isRandomControl Indique s'il s'agit d'un contrôle aléatoire
     */
    data class ControlState(
        val isSigned: Boolean = false,
        val hasPlanActions: Boolean = false,
        val isRandomControl: Boolean = false
    )

}
