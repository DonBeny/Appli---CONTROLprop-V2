package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData

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
        Log.d(TAG, "finishCtrl: Starting control finalization")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                manager.finishCtrl { success ->
                    _isLoading.postValue(false)

                    if (success) {
                        Log.d(TAG, "finishCtrl: Control finalized successfully")
                        _navigateToNext.postValue(true)
                    } else {
                        Log.e(TAG, "finishCtrl: Control finalization failed")
                        _error.postValue(Pair(ErrorCodes.SYNC_FAILED, "La synchronisation a échoué"))
                    }
                }
            } catch (e: BaseException) {
                Log.e(TAG, "finishCtrl: Error with code ${e.code}", e)
                _isLoading.postValue(false)
                _error.postValue(Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code)))
            } catch (e: Exception) {
                Log.e(TAG, "finishCtrl: Unexpected error", e)
                _isLoading.postValue(false)
                _error.postValue(Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite"))
            }
        }
    }



    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        Log.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
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
            Log.d(TAG, "setEntrySelected: Entry ${entrySelected.id} selected")
            generateBtnZones()
            refreshAllNotes()
        } catch (e: BaseException) {
            Log.e(TAG, "setEntrySelected: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            Log.e(TAG, "setEntrySelected: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
        }
    }



    private fun generateBtnZones() {
        try {
            _isLoading.value = true
            val btnZonesList = mutableListOf<ObjBtnZone>()

            userData.structure.forEach { (zoneId, structureZone) ->
                Log.d(TAG, "Checking zone ${zoneId}: ${structureZone.name}")

                val isProxiZone = withProxi && _residenceData.value?.prop?.zones?.proxi?.contains(zoneId) == true
                val isContractZone = withContract && _residenceData.value?.prop?.zones?.contra?.contains(zoneId) == true

                if (isProxiZone || isContractZone) {
                    Log.d(TAG, "generateBtnZones: Zone $zoneId is eligible")

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
            Log.d(TAG, "generateBtnZones: Generated ${btnZonesList.size} zone buttons")
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "generateBtnZones: Error generating zone buttons", e)
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

    fun updateZoneNote(zoneId: Int, elements: List<ObjElement>) {
        _isLoading.value = true
        Log.d(TAG, "updateZoneNote: Updating zone $zoneId with ${elements.size} elements")

        try {
            _residenceData.value?.let { currentEntry ->
                val updatedEntry = manager.updateGrilleData(currentEntry, zoneId, elements)
                val elementsMap = manager.getGrilleElements(updatedEntry)
                val zoneNote = calculateZoneNote(elements)
                val globalNote = calculateGlobalNote(elementsMap)

                Log.d(TAG, "updateZoneNote: Zone note: $zoneNote%, Global note: $globalNote%")

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
                Log.d(TAG, "updateZoneNote: Update successful")
            } ?: run {
                _isLoading.value = false
                Log.e(TAG, "updateZoneNote: No residence data available")
                throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Aucune résidence sélectionnée")
            }
        } catch (e: BaseException) {
            _isLoading.value = false
            Log.e(TAG, "updateZoneNote: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "updateZoneNote: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
        }
    }
    private fun updateZoneNoteUi(zoneId: Int, note: Int) {
        val updatedZones = _btnZones.value?.map { zone ->
            if (zone.id == zoneId) zone.copy(note = "$note%") else zone
        }
        _btnZones.value = updatedZones ?: emptyList()
    }
    private fun calculateZoneNote(elements: List<ObjElement>): Int {
        var sumValues = 0
        var sumCoefs = 0

        elements.forEach { element ->
            element.criterMap.values.forEach { critter ->
                if (critter.note == 1) sumValues += critter.coefProduct
                if (critter.note != 0) sumCoefs += critter.coefProduct
            }
        }

        return if (sumCoefs > 0) ((sumValues.toDouble() / sumCoefs) * 100).toInt() else 0
    }
    private fun calculateGlobalNote(elementsMap: Map<Int, List<ObjElement>>): Int {
        if (elementsMap.isEmpty()) return -1

        val zoneNotes = elementsMap.values.map { calculateZoneNote(it) }
        return zoneNotes.average().toInt()
    }
    fun refreshAllNotes() {
        Log.d(TAG, "refreshAllNotes: Refreshing all notes")

        try {
            _residenceData.value?.let { currentEntry ->
                val elementsMap = manager.getGrilleElements(currentEntry)

                val updatedZones = _btnZones.value?.map { zone ->
                    elementsMap[zone.id]?.let { elements ->
                        val zoneNote = calculateZoneNote(elements)
                        Log.d(TAG, "refreshAllNotes: Zone ${zone.id} note: $zoneNote%")
                        zone.copy(note = "$zoneNote%")
                    } ?: zone
                } ?: emptyList()
                _btnZones.value = updatedZones

                val globalNote = calculateGlobalNote(elementsMap)
                _noteCtrl.value = if (globalNote < 0) "S O" else "$globalNote%"
                Log.d(TAG, "refreshAllNotes: Global note: $globalNote%")

                _residenceData.value = currentEntry.copy(
                    prop = currentEntry.prop?.copy(
                        ctrl = currentEntry.prop.ctrl.copy(
                            note = globalNote
                        )
                    )
                )
            }
        } catch (e: BaseException) {
            Log.e(TAG, "refreshAllNotes: Error with code ${e.code}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            Log.e(TAG, "refreshAllNotes: Unexpected error", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Une erreur inattendue s'est produite")
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
