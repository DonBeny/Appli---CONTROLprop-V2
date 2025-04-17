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
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.utils.LogUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PlanActionsViewModel(
    private val planActionsManager: PlanActionsManager,
    private val syncManager: SyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "PlanActionsViewModel"

        const val DESTINATION_SELECT_ENTRY = "select_entry"
        const val DESTINATION_BACK = "back"
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
        data object NavigateBack : PlanActionEvent()
        data class NavigateTo(val destination: String) : PlanActionEvent()
        data object OpenCalendar : PlanActionEvent()
        data class ShowMessage(val message: String) : PlanActionEvent()
        data object ResetForm : PlanActionEvent()
        data object StayOnScreen : PlanActionEvent()
    }

    private var idMbr: Int = -1
    private var adrMac: String = ""
    private var idRsd: Int = -1
    private var entrySelected: SelectItem? = null
    private var typeCtrl: String? = null

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
        LogUtils.d(TAG, "Credentials set: idMbr=$idMbr, adrMac=$adrMac, idRsd=$idRsd")
    }

    fun setTypeCtrl(type: String?) {
        this.typeCtrl = type
        LogUtils.d(TAG, "Type de contrôle défini: $type")
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

                LogUtils.json(TAG, "getPlanActions: response", response)
                LogUtils.json(TAG, "getPlanActions: planAction", planAction)

                _currentPlan.value = planAction
                _state.value = PlanActionState.PlanLoaded(planAction)

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

                entrySelected = entrySelected?.copy(
                    planActions = planAction,
                    saved = false
                )

                if (entrySelected == null) {
                    _state.value = PlanActionState.Error(
                        "Aucune résidence sélectionnée",
                        ErrorCodes.INVALID_DATA
                    )
                    return@launch
                }


                try {
                    syncManager.addOrUpdatePendingControl(entrySelected!!)

                    when (val result = syncManager.syncPendingControls()) {
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
                        SyncManager.SyncResult.NO_NETWORK -> {
                            _state.value = PlanActionState.Error(
                                "Pas de connexion réseau. Plan d'actions enregistré localement.",
                                ErrorCodes.NETWORK_ERROR
                            )
                        }
                        else -> {
                            _state.value = PlanActionState.Error(
                                "Erreur lors de la synchronisation",
                                ErrorCodes.SYNC_FAILED
                            )
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "Erreur lors de la synchronisation", e)
                    _state.value = PlanActionState.Error(
                        "Erreur lors de la synchronisation: ${e.message}",
                        ErrorCodes.SYNC_FAILED
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur lors de la sauvegarde du plan d'actions", e)
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

                entrySelected = entrySelected?.copy(
                    planActions = planAction,
                    saved = false
                )

                if (entrySelected == null) {
                    _state.value = PlanActionState.Error(
                        "Aucune résidence sélectionnée",
                        ErrorCodes.INVALID_DATA
                    )
                    return@launch
                }

                try {
                    syncManager.addOrUpdatePendingControl(entrySelected!!)

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
                        SyncManager.SyncResult.NO_NETWORK -> {
                            _state.value = PlanActionState.Error(
                                "Pas de connexion réseau. Plan d'actions enregistré localement.",
                                ErrorCodes.NETWORK_ERROR
                            )
                            // On peut toujours ouvrir le calendrier même sans connexion
                            _event.value = PlanActionEvent.OpenCalendar
                        }
                        else -> {
                            _state.value = PlanActionState.Error(
                                "Erreur lors de la synchronisation",
                                ErrorCodes.SYNC_FAILED
                            )
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "Erreur lors de la synchronisation", e)
                    _state.value = PlanActionState.Error(
                        "Erreur lors de la synchronisation: ${e.message}",
                        ErrorCodes.SYNC_FAILED
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur lors de la sauvegarde du plan d'actions avec rappel", e)
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
                val response = planActionsManager.validatePlanAction(idPlan, idMbr, adrMac)

                LogUtils.json(TAG, "validatePlanAction: response", response)

                if (response.optBoolean("status", false)) {
                    LogUtils.d(TAG, "Plan d'actions validé avec succès via API")

                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                    val tomorrowDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(tomorrow.time)

                    val emptyPlan = ObjPlanActions(
                        id = 0,
                        limit = tomorrowDate,
                        txt = "",
                        isLevee = false
                    )

                    entrySelected = entrySelected?.copy(
                        planActions = emptyPlan,
                        saved = true
                    )

                    entrySelected?.let {
                        syncManager.addOrUpdatePendingControl(it)
                    }

                    _currentPlan.value = null
                    _state.value = PlanActionState.Success("Plan d'actions levé avec succès")

                    if (typeCtrl == TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE) {
                        _event.value = PlanActionEvent.NavigateTo(DESTINATION_SELECT_ENTRY)
                    } else {
                        _event.value = PlanActionEvent.StayOnScreen
                        _mode.value = PlanActionMode.CREATION
                        _event.value = PlanActionEvent.ResetForm
                    }
                } else {
                    val message = response.optString("error", "Erreur lors de la validation du plan d'actions")
                    LogUtils.e(TAG, "Échec de la validation du plan d'actions: $message")
                    _state.value = PlanActionState.Error(
                        message,
                        ErrorCodes.INVALID_RESPONSE
                    )
                }
            } catch (e: PlanActionsManager.PlanActionsException) {
                LogUtils.e(TAG, "Erreur lors de la validation du plan d'actions", e)

                if (e.code == ErrorCodes.NETWORK_ERROR) {
                    // Cas spécial pour les erreurs réseau
                    _state.value = PlanActionState.Error(
                        "Pas de connexion réseau. Impossible de valider le plan d'actions.",
                        ErrorCodes.NETWORK_ERROR
                    )
                } else {
                    _state.value = PlanActionState.Error(
                        e.message ?: "Erreur lors de la validation",
                        e.code
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur inattendue lors de la validation du plan d'actions", e)
                _state.value = PlanActionState.Error(
                    e.message ?: "Erreur inattendue lors de la validation",
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
                val txt = dataObject.getString("txt")

                LogUtils.json(TAG, "parsePlanActions: dataObject", dataObject)

                val limit = if (dataObject.has("limit")) {
                    try {
                        val timestamp = dataObject.getLong("limit")
                        formatTimestampToDate(timestamp)
                    } catch (e: Exception) {
                        dataObject.getString("limit")
                    }
                } else {
                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                    SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(tomorrow.time)
                }

                LogUtils.d(TAG, "parsePlanActions: limit: $limit")

                ObjPlanActions(id, limit, txt)
            } else {
                null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Erreur lors du parsing du plan d'actions", e)
            null
        }
    }
    /**
     * Convertit un timestamp PHP (en secondes) en date formatée.
     *
     * @param timestamp Le timestamp PHP (en secondes)
     * @return La date formatée au format "dd/MM/yyyy"
     */
    private fun formatTimestampToDate(timestamp: Long): String {
        val timestampInMillis = if (timestamp < 10000000000L) {
            timestamp * 1000
        } else {
            timestamp
        }

        val timeZone = TimeZone.getTimeZone("Europe/Paris")
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = timestampInMillis

        LogUtils.d(TAG, "Timestamp original: $timestamp")
        LogUtils.d(TAG, "Timestamp en millisecondes: $timestampInMillis")
        LogUtils.d(TAG, "Date: ${calendar.time}")

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)

        dateFormat.timeZone = timeZone

        return dateFormat.format(calendar.time)
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
            LogUtils.json(TAG, "Contrôles en attente mis à jour: ${pendingControls.size} contrôles", pendingControls)
        }
    }

}
