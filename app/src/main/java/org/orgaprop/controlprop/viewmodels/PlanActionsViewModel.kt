package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.orgaprop.controlprop.managers.PlanActionsManager
import org.orgaprop.controlprop.models.ObjPlanActions

class PlanActionsViewModel(private val planActionsManager: PlanActionsManager) : ViewModel() {

    private val TAG = "PlanActionsViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""
    private var idRsd: Int = -1

    private val _planActions = MutableLiveData<ObjPlanActions>()
    val planActions: LiveData<ObjPlanActions> get() = _planActions

    private val _savePlanActionResult = MutableLiveData<Boolean>()
    val savePlanActionResult: LiveData<Boolean> get() = _savePlanActionResult

    private val _openCalendarEvent = MutableLiveData<Boolean>()
    val openCalendarEvent: LiveData<Boolean> get() = _openCalendarEvent

    private val savePlanActionResultMediator = MediatorLiveData<Boolean>()

    private val _validatePlanActionResult = MutableLiveData<Boolean>()
    val validatePlanActionResult: LiveData<Boolean> get() = _validatePlanActionResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        savePlanActionResultMediator.addSource(savePlanActionResult) { success ->
            savePlanActionResultMediator.value = success
        }
    }

    fun getPlanActions() {
        viewModelScope.launch {
            try {
                val response = planActionsManager.fetchPlanActions(idRsd, idMbr, adrMac)
                val planAction = parsePlanActions(response)

                if (planAction != null) {
                    _planActions.value = planAction!!
                } else {
                    _errorMessage.value = "Aucune donnée valide reçue"
                }
            } catch (e: PlanActionsManager.PlanActionsException) {
                _errorMessage.value = e.message ?: "Erreur inconnue"
            }
        }
    }

    fun savePlanAction(idPlan: Int, limit: String, txt: String) {
        viewModelScope.launch {
            try {
                val planAction = ObjPlanActions(
                    id = idPlan,
                    limit = limit,
                    txt = txt
                )
                val response = planActionsManager.savePlanAction(planAction, idRsd, idMbr, adrMac)

                _savePlanActionResult.value = response.getBoolean("status")
            } catch (e: PlanActionsManager.PlanActionsException) {
                _errorMessage.value = e.message ?: "Erreur inconnue"
            }
        }
    }
    fun savePlanActionAndOpenCalendar(idPlanActions: Int, date: String, planText: String) {
        viewModelScope.launch {
            try {
                savePlanAction(idPlanActions, date, planText)

                savePlanActionResultMediator.observeForever { success ->
                    if (success) {
                        _openCalendarEvent.value = true // Ouvrir l'agenda si la sauvegarde réussit
                    } else {
                        _errorMessage.value = "Erreur lors de l'enregistrement du plan d'actions"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur inconnue"
            }
        }
    }

    fun validatePlanAction(idPlan: Int) {
        viewModelScope.launch {
            try {
                val response = planActionsManager.validatePlanAction(idPlan, idMbr, adrMac)

                _validatePlanActionResult.value = response.getBoolean("status")
            } catch (e: PlanActionsManager.PlanActionsException) {
                _errorMessage.value = e.message ?: "Erreur inconnue"
            }
        }
    }

    private fun parsePlanActions(response: JSONObject): ObjPlanActions? {
        return try {
            if (response.getBoolean("status")) {
                val dataObject = response.getJSONObject("data")
                val id = dataObject.getInt("id")
                val limit = dataObject.getString("limit")
                val txt = dataObject.getString("txt")

                ObjPlanActions(id, limit, txt)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setUserCredentials(idMbr: Int, adrMac: String, idRsd: Int) {
        this.idMbr = idMbr
        this.adrMac = adrMac
        this.idRsd = idRsd

        Log.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}, idRsd: ${this.idRsd}")
    }

}
