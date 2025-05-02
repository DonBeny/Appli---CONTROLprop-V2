package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.LogUtils

class GrilleCtrlViewModel(private val manager: GrilleCtrlManager) : ViewModel() {

    private val TAG = "GrilleCtrlViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private lateinit var userData: LoginData
    private var withProxi: Boolean = false
    private var withContract: Boolean = false

    private val _btnZones = MutableLiveData<List<ObjBtnZone>>()
    val btnZones: LiveData<List<ObjBtnZone>> get() = _btnZones

    private val _residenceData = MutableLiveData<SelectItem>()
    val residenceData: LiveData<SelectItem> get() = _residenceData

    private val _noteCtrl = MutableLiveData<String>()
    val noteCtrl: LiveData<String> get() = _noteCtrl

    private val _navigateToNext = MutableLiveData<Boolean>()
    val navigateToNext: LiveData<Boolean> get() = _navigateToNext

    private val _error = MutableLiveData<Pair<Int, String>?>()
    val error: LiveData<Pair<Int, String>?> get() = _error

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading



    fun finishCtrl() {
        LogUtils.d(TAG, "finishCtrl: Starting control finalization")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                manager.finishCtrl { success ->
                    _isLoading.postValue(false)

                    if (success) {
                        LogUtils.d(TAG, "finishCtrl: Control finalized successfully")
                        _navigateToNext.postValue(true)
                    } else {
                        LogUtils.e(TAG, "finishCtrl: Control finalization failed (likely network issue)")
                        _error.postValue(Pair(ErrorCodes.NETWORK_ERROR, "Problème de connexion lors de la synchronisation"))
                        _navigateToNext.postValue(true)
                    }
                }
            } catch (e: BaseException) {
                LogUtils.e(TAG, "finishCtrl: Error with code ${e.code}", e)
                _isLoading.postValue(false)
                _error.postValue(Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code)))
                _navigateToNext.postValue(true)
            } catch (e: Exception) {
                LogUtils.e(TAG, "finishCtrl: Unexpected error", e)
                _error.postValue(Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite"))
                _navigateToNext.postValue(true)
            }
        }
    }



    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        LogUtils.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
    }
    fun setUserData(userData: LoginData, withProxi: Boolean, withContract: Boolean) {
        setUserCredentials(userData.idMbr, userData.adrMac)
        this.userData = userData
        this.withProxi = withProxi
        this.withContract = withContract
    }
    fun setEntrySelected(entrySelected: SelectItem) {
        try {
            _residenceData.value = entrySelected
            LogUtils.json(TAG, "setEntrySelected: Entry selected", entrySelected)
            generateBtnZones()
            refreshAllNotes()
        } catch (e: BaseException) {
            LogUtils.e(TAG, "setEntrySelected: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            LogUtils.e(TAG, "setEntrySelected: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
        }
    }



    private fun generateBtnZones() {
        try {
            _isLoading.value = true
            val btnZonesList = mutableListOf<ObjBtnZone>()

            userData.structure.forEach { (zoneId, structureZone) ->
                LogUtils.json(TAG, "generateBtnZones: Checking zone ${zoneId}:", structureZone)

                val isProxiZone = withProxi && _residenceData.value?.prop?.zones?.proxi?.contains(zoneId) == true
                val isContractZone = withContract && _residenceData.value?.prop?.zones?.contra?.contains(zoneId) == true

                if (isProxiZone || isContractZone) {
                    LogUtils.d(TAG, "generateBtnZones: Zone $zoneId is eligible")

                    val btnZone = ObjBtnZone(
                        id = zoneId.toInt(),
                        txt = structureZone.name,
                        note = "S O",
                        icon = getIconForZone(zoneId.toInt())
                    )

                    btnZonesList.add(btnZone)
                }
            }

            _btnZones.value = btnZonesList
            _isLoading.value = false
            LogUtils.json(TAG, "generateBtnZones: Generated ${btnZonesList.size} zone buttons", btnZonesList)
        } catch (e: Exception) {
            _isLoading.value = false
            LogUtils.e(TAG, "generateBtnZones: Error generating zone buttons", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la génération des boutons de zone", e)
        }
    }
    private fun getIconForZone(zoneId: Int): Int {
        return when (zoneId) {
            1 -> R.drawable.abords_acces_immeubles_vert
            2 -> R.drawable.hall_vert
            3 -> R.drawable.ascenseur_vert
            4 -> R.drawable.escalier_vert
            5 -> R.drawable.paliers_coursives_vert
            6 -> R.drawable.local_om_vert
            7 -> R.drawable.local_velo_vert
            8 -> R.drawable.cave_vert
            9 -> R.drawable.parking_sous_sol_vert
            10 -> R.drawable.cour_interieure_vert
            11 -> R.drawable.parking_exterieur_vert
            12 -> R.drawable.espaces_exterieurs_vert
            13 -> R.drawable.agence_vert
            14 -> R.drawable.salle_commune_vert
            15 -> R.drawable.buanderie_vert
            16 -> R.drawable.ascenseur_vert
            17 -> R.drawable.local_om_vert
            18 -> R.drawable.local_poussette_vert
            19 -> R.drawable.paliers_coursives_vert
            else -> R.drawable.localisation_vert
        }
    }
    /**
     * Récupère le coefficient d'une zone à partir de la structure de l'utilisateur
     *
     * @param zoneId L'identifiant de la zone
     * @return Le coefficient de la zone, ou 1 si non trouvé (pour ne pas affecter le calcul)
     */
    private fun getZoneCoefficient(zoneId: Int): Int {
        val zoneIdStr = zoneId.toString()

        return try {
            userData.structure[zoneIdStr]?.coef ?: 1
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error getting zone coefficient for zone $zoneId", e)
            1
        }
    }

    fun updateZoneNote(zoneId: Int, elements: List<ObjElement>) {
        _isLoading.value = true
        LogUtils.json(TAG, "updateZoneNote: Updating zone $zoneId with ${elements.size} elements", elements)

        try {
            _residenceData.value?.let { currentEntry ->
                val updatedEntry = manager.updateGrilleData(currentEntry, zoneId, elements)
                val elementsMap = manager.getGrilleElements(updatedEntry)
                val zoneNote = calculateZoneNote(elements, zoneId)
                val globalNote = calculateGlobalNote(elementsMap)

                LogUtils.d(TAG, "updateZoneNote: Zone note: $zoneNote%, Global note: $globalNote%")

                val finalEntry = updatedEntry.copy(
                    prop = updatedEntry.prop?.copy(
                        ctrl = updatedEntry.prop.ctrl.copy(
                            note = globalNote
                        )
                    )
                )

                manager.saveControlProgress(finalEntry)

                _residenceData.value = finalEntry
                _noteCtrl.value = "$globalNote%"
                updateZoneNoteUi(zoneId, zoneNote)

                _isLoading.value = false
                LogUtils.d(TAG, "updateZoneNote: Update successful")
            } ?: run {
                _isLoading.value = false
                LogUtils.e(TAG, "updateZoneNote: No residence data available")
                throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Aucune résidence sélectionnée")
            }
        } catch (e: BaseException) {
            _isLoading.value = false
            LogUtils.e(TAG, "updateZoneNote: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            _isLoading.value = false
            LogUtils.e(TAG, "updateZoneNote: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
        }
    }
    private fun updateZoneNoteUi(zoneId: Int, note: Int) {
        val updatedZones = _btnZones.value?.map { zone ->
            if (zone.id == zoneId) zone.copy(note = "$note%") else zone
        }
        _btnZones.value = updatedZones ?: emptyList()
    }
    /**
     * Calcule la note d'une zone en prenant en compte le coefficient de la zone
     *
     * @param elements La liste des éléments de la zone
     * @param zoneId L'identifiant de la zone
     * @return La note calculée de la zone (0-100 ou -1 si non évaluée)
     */
    private fun calculateZoneNote(elements: List<ObjElement>, zoneId: Int): Int {
        var sumValues = 0
        var sumCoefs = 0
        val coefZone = getZoneCoefficient(zoneId)

        LogUtils.d(TAG, "calculateZoneNote: Zone $zoneId has coefficient: $coefZone")

        elements.forEach { element ->
            element.criterMap.values.forEach { critter ->
                val totalCoef = coefZone * critter.coefProduct

                if (critter.note == 1) sumValues += totalCoef
                if (critter.note != 0) sumCoefs += totalCoef
            }
        }

        LogUtils.d(TAG, "calculateZoneNote: Zone $zoneId - sumValues: $sumValues, sumCoefs: $sumCoefs")

        return if (sumCoefs > 0) ((sumValues.toDouble() / sumCoefs) * 100).toInt() else -1
    }
    /**
     * Calcule la note globale en prenant en compte toutes les zones et leurs coefficients
     *
     * @param elementsMap La carte des éléments par zone
     * @return La note globale (0-100 ou -1 si aucune zone évaluée)
     */
    private fun calculateGlobalNote(elementsMap: Map<Int, List<ObjElement>>): Int {
        if (elementsMap.isEmpty()) return -1

        var totalSumValues = 0
        var totalSumCoefs = 0

        elementsMap.forEach { (zoneId, elements) ->
            val coefZone = getZoneCoefficient(zoneId)

            LogUtils.d(TAG, "calculateGlobalNote: Zone $zoneId has coefficient: $coefZone")

            elements.forEach { element ->
                element.criterMap.values.forEach { critter ->
                    val totalCoef = coefZone * critter.coefProduct

                    if (critter.note == 1) {
                        totalSumValues += totalCoef
                        totalSumCoefs += totalCoef
                    } else if (critter.note == -1) {
                        totalSumCoefs += totalCoef
                    }
                }
            }
        }

        LogUtils.d(TAG, "calculateGlobalNote: Total sumValues: $totalSumValues, total sumCoefs: $totalSumCoefs")

        return if (totalSumCoefs > 0) ((totalSumValues.toDouble() / totalSumCoefs) * 100).toInt() else -1
    }
    fun refreshAllNotes() {
        LogUtils.d(TAG, "refreshAllNotes: Refreshing all notes")

        try {
            _residenceData.value?.let { currentEntry ->
                val elementsMap = manager.getGrilleElements(currentEntry)

                val updatedZones = _btnZones.value?.map { zone ->
                    elementsMap[zone.id]?.let { elements ->
                        val zoneNote = calculateZoneNote(elements, zone.id)

                        LogUtils.d(TAG, "refreshAllNotes: Zone ${zone.id} note: $zoneNote%")

                        zone.copy(note = "$zoneNote%")
                    } ?: zone
                } ?: emptyList()
                _btnZones.value = updatedZones

                val globalNote = calculateGlobalNote(elementsMap)
                _noteCtrl.value = if (globalNote < 0) "S O" else "$globalNote%"

                LogUtils.d(TAG, "refreshAllNotes: Global note: $globalNote%")

                _residenceData.value = currentEntry.copy(
                    prop = currentEntry.prop?.copy(
                        ctrl = currentEntry.prop.ctrl.copy(
                            note = globalNote
                        )
                    )
                )
            }
        } catch (e: BaseException) {
            LogUtils.e(TAG, "refreshAllNotes: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            LogUtils.e(TAG, "refreshAllNotes: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
        }
    }



    fun removeZone(zoneId: Int) {
        _isLoading.value = true
        try {
            _residenceData.value?.let { currentEntry ->
                // Obtenir la grille actuelle
                val currentGrille = JSONArray(currentEntry.prop?.ctrl?.grille ?: "[]")

                // Créer une nouvelle grille sans la zone spécifiée
                val updatedGrille = JSONArray()
                for (i in 0 until currentGrille.length()) {
                    val zoneObj = currentGrille.getJSONObject(i)
                    if (zoneObj.getInt("zoneId") != zoneId) {
                        updatedGrille.put(zoneObj)
                    }
                }

                // Mettre à jour l'entrée avec la nouvelle grille
                val updatedEntry = currentEntry.copy(
                    prop = currentEntry.prop?.copy(
                        ctrl = currentEntry.prop.ctrl.copy(
                            grille = updatedGrille.toString()
                        )
                    )
                )

                // Recalculer la note globale
                val elementsMap = manager.getGrilleElements(updatedEntry)
                val globalNote = calculateGlobalNote(elementsMap)

                val finalEntry = updatedEntry.copy(
                    prop = updatedEntry.prop?.copy(
                        ctrl = updatedEntry.prop.ctrl.copy(
                            note = globalNote
                        )
                    )
                )

                // Sauvegarder les modifications
                manager.saveControlProgress(finalEntry)

                _residenceData.value = finalEntry
                _noteCtrl.value = if (globalNote < 0) "S O" else "$globalNote%"

                // Mettre à jour l'UI
                val updatedZones = _btnZones.value?.map { zone ->
                    if (zone.id == zoneId) zone.copy(note = "S O") else zone
                }
                _btnZones.value = updatedZones ?: emptyList()
            }
            _isLoading.value = false
        } catch (e: Exception) {
            _isLoading.value = false
            // Gestion des erreurs
        }
    }



    fun clearError() {
        _error.value = null
    }



    sealed class NavigationDestination {
        data object NextScreen : NavigationDestination()
        // Ajoutez d'autres destinations au besoin
    }

}
