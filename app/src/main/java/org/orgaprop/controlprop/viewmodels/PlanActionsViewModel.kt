package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.PlanActionsManager
import org.orgaprop.controlprop.models.ObjPlanActions
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.sync.SyncManager

class PlanActionsViewModel(
    private val planActionsManager: PlanActionsManager,
    private val syncManager: SyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "PlanActionsViewModel"
    }

    /**
     * Représente les différents états possibles de l'écran de plan d'actions.
     */
    sealed class PlanActionState {
        object Loading : PlanActionState()
        data class PlanLoaded(val plan: ObjPlanActions?) : PlanActionState()
        data class Success(val message: String) : PlanActionState()
        data class Error(val message: String, val code: Int? = null) : PlanActionState()
    }

    /**
     * Représente les différents modes d'affichage de l'écran de plan d'actions.
     */
    enum class PlanActionMode {
        CREATION, // Création d'un nouveau plan d'actions
        EDITION    // Visualisation/édition d'un plan existant
    }

    /**
     * Représente les différentes actions possibles suite à une interaction utilisateur.
     */
    sealed class PlanActionEvent {
        object NavigateBack : PlanActionEvent()
        object OpenCalendar : PlanActionEvent()
        data class ShowMessage(val message: String) : PlanActionEvent()
        object ResetForm : PlanActionEvent()
    }

    private var idMbr: Int = -1
    private var adrMac: String = ""
    private var idRsd: Int = -1
    private var entrySelected: SelectItem? = null

    // État actuel du plan d'actions
    private val _state = MutableLiveData<PlanActionState>(PlanActionState.Loading)
    val state: LiveData<PlanActionState> = _state

    // Mode actuel (création ou édition)
    private val _mode = MutableLiveData<PlanActionMode>(PlanActionMode.CREATION)
    val mode: LiveData<PlanActionMode> = _mode

    // Événements UI à déclencher
    private val _event = MutableLiveData<PlanActionEvent?>()
    val event: LiveData<PlanActionEvent?> = _event

    // Le plan d'actions actuel
    private val _currentPlan = MutableLiveData<ObjPlanActions?>()
    val currentPlan: LiveData<ObjPlanActions?> = _currentPlan



    /**
     * Initialise les informations de l'utilisateur et de la résidence.
     */
    fun setUserCredentials(idMbr: Int, adrMac: String, rsd: SelectItem) {
        this.idMbr = idMbr
        this.adrMac = adrMac
        this.idRsd = rsd.id
        this.entrySelected = rsd
        Log.d(TAG, "Credentials set: idMbr=$idMbr, adrMac=$adrMac, idRsd=$idRsd")
    }

    /**
     * Récupère le plan d'actions actuel pour la résidence sélectionnée.
     */
    fun getPlanActions() {
        if (idMbr <= 0 || adrMac.isEmpty() || idRsd <= 0) {
            _state.value = PlanActionState.Error(
                "Informations d'identification invalides",
                ErrorCodes.INVALID_INPUT
            )
            return
        }

        viewModelScope.launch {
            _state.value = PlanActionState.Loading

            try {
                val response = planActionsManager.fetchPlanActions(idRsd, idMbr, adrMac)
                val planAction = parsePlanActions(response)

                _currentPlan.value = planAction
                _state.value = PlanActionState.PlanLoaded(planAction)

                // Mise à jour du mode selon l'existence d'un plan
                _mode.value = if (planAction != null && planAction.id > 0) {
                    PlanActionMode.EDITION
                } else {
                    PlanActionMode.CREATION
                }

            } catch (e: PlanActionsManager.PlanActionsException) {
                Log.e(TAG, "Erreur lors de la récupération du plan d'actions", e)
                _state.value = PlanActionState.Error(e.message ?: "Erreur inconnue", e.code)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors de la récupération du plan d'actions", e)
                _state.value = PlanActionState.Error(
                    "Une erreur inattendue est survenue",
                    ErrorCodes.UNKNOWN_ERROR
                )
            }
        }
    }

    /**
     * Sauvegarde un plan d'actions.
     */
    fun savePlanAction(idPlan: Int, limit: String, txt: String) {
        if (limit.isBlank() || txt.isBlank()) {
            _state.value = PlanActionState.Error(
                "Veuillez remplir tous les champs",
                ErrorCodes.INVALID_INPUT
            )
            return
        }

        viewModelScope.launch {
            _state.value = PlanActionState.Loading

            try {
                val planAction = ObjPlanActions(
                    id = idPlan,
                    limit = limit,
                    txt = txt,
                    isLevee = false
                )

                // Mise à jour du plan dans l'entrée sélectionnée
                entrySelected = entrySelected?.copy(
                    planActions = planAction,
                    saved = false
                )

                // Mise à jour dans la liste des contrôles en attente
                updatePendingControls()

                // Déclenchement de la synchronisation
                val result = syncManager.syncPendingControls()

                when (result) {
                    SyncManager.SyncResult.SUCCESS -> {
                        _state.value = PlanActionState.Success("Plan d'actions enregistré avec succès")
                        _event.value = PlanActionEvent.NavigateBack
                    }
                    is SyncManager.SyncResult.PARTIAL_SUCCESS -> {
                        if (!result.errors.any { error -> error.id == entrySelected?.id }) {
                            _state.value = PlanActionState.Success("Plan d'actions enregistré avec succès")
                            _event.value = PlanActionEvent.NavigateBack
                        } else {
                            _state.value = PlanActionState.Error(
                                "Erreur lors de la synchronisation du plan d'actions",
                                ErrorCodes.SYNC_FAILED
                            )
                        }
                    }
                    else -> {
                        _state.value = PlanActionState.Error(
                            "Erreur lors de la synchronisation",
                            ErrorCodes.SYNC_FAILED
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sauvegarde du plan d'actions", e)
                _state.value = PlanActionState.Error(
                    e.message ?: "Erreur lors de la sauvegarde",
                    ErrorCodes.UNKNOWN_ERROR
                )
            }
        }
    }

    /**
     * Sauvegarde un plan d'actions et ouvre le calendrier pour créer un rappel.
     */
    fun savePlanActionAndOpenCalendar(idPlan: Int, limit: String, txt: String) {
        if (limit.isBlank() || txt.isBlank()) {
            _state.value = PlanActionState.Error(
                "Veuillez remplir tous les champs",
                ErrorCodes.INVALID_INPUT
            )
            return
        }

        viewModelScope.launch {
            _state.value = PlanActionState.Loading

            try {
                val planAction = ObjPlanActions(
                    id = idPlan,
                    limit = limit,
                    txt = txt,
                    isLevee = false
                )

                // Mise à jour du plan dans l'entrée sélectionnée
                entrySelected = entrySelected?.copy(
                    planActions = planAction,
                    saved = false
                )

                // Mise à jour dans la liste des contrôles en attente
                updatePendingControls()

                // Déclenchement de la synchronisation
                val result = syncManager.syncPendingControls()

                when (result) {
                    SyncManager.SyncResult.SUCCESS -> {
                        _state.value = PlanActionState.Success("Plan d'actions enregistré avec succès")
                        _event.value = PlanActionEvent.OpenCalendar
                    }
                    is SyncManager.SyncResult.PARTIAL_SUCCESS -> {
                        if (!result.errors.any { error -> error.id == entrySelected?.id }) {
                            _state.value = PlanActionState.Success("Plan d'actions enregistré avec succès")
                            _event.value = PlanActionEvent.OpenCalendar
                        } else {
                            _state.value = PlanActionState.Error(
                                "Erreur lors de la synchronisation du plan d'actions",
                                ErrorCodes.SYNC_FAILED
                            )
                        }
                    }
                    else -> {
                        _state.value = PlanActionState.Error(
                            "Erreur lors de la synchronisation",
                            ErrorCodes.SYNC_FAILED
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sauvegarde du plan d'actions avec rappel", e)
                _state.value = PlanActionState.Error(
                    e.message ?: "Erreur lors de la sauvegarde",
                    ErrorCodes.UNKNOWN_ERROR
                )
            }
        }
    }

    /**
     * Valide (lève) un plan d'actions existant.
     */
    fun validatePlanAction(idPlan: Int) {
        if (idPlan <= 0) {
            _state.value = PlanActionState.Error(
                "ID du plan d'actions invalide",
                ErrorCodes.INVALID_INPUT
            )
            return
        }

        viewModelScope.launch {
            _state.value = PlanActionState.Loading

            try {
                // Mise à jour du plan d'actions avec isLevee = true
                entrySelected?.planActions?.let { currentPlan ->
                    val updatedPlan = currentPlan.copy(isLevee = true)

                    entrySelected = entrySelected?.copy(
                        planActions = updatedPlan,
                        saved = false
                    )

                    // Mise à jour dans la liste des contrôles en attente
                    updatePendingControls()

                    // Déclenchement de la synchronisation
                    val result = syncManager.syncPendingControls()

                    when (result) {
                        SyncManager.SyncResult.SUCCESS -> {
                            _state.value = PlanActionState.Success("Plan d'actions levé avec succès")
                            _event.value = PlanActionEvent.ResetForm
                            _mode.value = PlanActionMode.CREATION
                        }
                        is SyncManager.SyncResult.PARTIAL_SUCCESS -> {
                            if (!result.errors.any { error -> error.id == entrySelected?.id }) {
                                _state.value = PlanActionState.Success("Plan d'actions levé avec succès")
                                _event.value = PlanActionEvent.ResetForm
                                _mode.value = PlanActionMode.CREATION
                            } else {
                                _state.value = PlanActionState.Error(
                                    "Erreur lors de la synchronisation de la levée",
                                    ErrorCodes.SYNC_FAILED
                                )
                            }
                        }
                        else -> {
                            _state.value = PlanActionState.Error(
                                "Erreur lors de la synchronisation",
                                ErrorCodes.SYNC_FAILED
                            )
                        }
                    }
                } ?: run {
                    _state.value = PlanActionState.Error(
                        "Aucun plan d'actions à lever",
                        ErrorCodes.DATA_NOT_FOUND
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la validation du plan d'actions", e)
                _state.value = PlanActionState.Error(
                    e.message ?: "Erreur lors de la validation",
                    ErrorCodes.UNKNOWN_ERROR
                )
            }
        }
    }



    /**
     * Signale que l'événement UI a été traité.
     */
    fun eventHandled() {
        _event.value = null
    }



    /**
     * Parse la réponse du serveur pour extraire les informations du plan d'actions.
     */
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
            Log.e(TAG, "Erreur lors du parsing du plan d'actions", e)
            null
        }
    }

    /**
     * Met à jour la liste des contrôles en attente avec l'entrée sélectionnée modifiée.
     */
    private fun updatePendingControls() {
        entrySelected?.let { entry ->
            val pendingControls = syncManager.getPendingControls().toMutableList()

            val index = pendingControls.indexOfFirst { it.id == entry.id }
            if (index != -1) {
                pendingControls[index] = entry
            } else {
                pendingControls.add(entry)
            }

            syncManager.savePendingControls(pendingControls)
            Log.d(TAG, "Contrôles en attente mis à jour: ${pendingControls.size} contrôles")
        }
    }

}
