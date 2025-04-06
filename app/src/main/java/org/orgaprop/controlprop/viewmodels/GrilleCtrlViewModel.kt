package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.main.types.LoginData

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



    fun finishCtrl() {
        viewModelScope.launch {
            Log.d(TAG, "finishCtrl: launch Finishing ctrl")
            manager.finishCtrl { success ->
                Log.d(TAG, "finishCtrl: Finishing ctrl done")
                _navigateToNext.postValue(success)
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
        _residenceData.value = entrySelected
        generateBtnZones()
        refreshAllNotes()
    }



    private fun generateBtnZones() {
        val btnZonesList = mutableListOf<ObjBtnZone>()

        userData.structure.forEach { (zoneId, structureZone) ->
            Log.d(TAG, "Checking zone: ${structureZone.name} - proxi:$withProxi contract:$withContract")

            val isProxiZone = withProxi && _residenceData.value?.prop?.zones?.proxi?.contains(zoneId) == true
            val isContractZone = withContract && _residenceData.value?.prop?.zones?.contra?.contains(zoneId) == true

            if (isProxiZone || isContractZone) {
                Log.d(TAG, "generateBtnZones: Adding zone to list")

                val btnZone = ObjBtnZone(
                    id = zoneId.toInt(),
                    txt = structureZone.name,
                    note = "S O",
                    icon = getIconForZone(zoneId.toInt())
                )

                Log.d(TAG, "generateBtnZones: btnZone: $btnZone")

                btnZonesList.add(btnZone)
            }
        }

        _btnZones.value = btnZonesList
    }
    private fun getIconForZone(zoneId: Int): String {
        return when (zoneId) {
            1 -> "abords_acces_immeubles_vert"
            2 -> "hall_vert"
            3 -> "ascenseur_vert"
            4 -> "escalier_vert"
            5 -> "paliers_coursives_vert"
            6 -> "local_om_vert"
            7 -> "local_velo_vert"
            8 -> "cave_vert"
            9 -> "parking_sous_sol_vert"
            10 -> "cour_interieure_vert"
            11 -> "parking_exterieur_vert"
            12 -> "espaces_exterieurs_vert"
            13 -> "agence_vert"
            14 -> "salle_commune_vert"
            15 -> "buanderie_vert"
            16 -> "ascenseur_vert"
            17 -> "local_om_vert"
            18 -> "local_poussette_vert"
            19 -> "paliers_coursives_vert"
            else -> "localisation_vert"
        }
    }

    fun updateZoneNote(zoneId: Int, elements: List<ObjElement>) {
        Log.d(TAG, "updateZoneNote: zoneId: $zoneId, elements: $elements")

        _residenceData.value?.let { currentEntry ->
            Log.d(TAG, "updateZoneNote: currentEntry: $currentEntry")

            val updatedEntry = manager.updateGrilleData(currentEntry, zoneId, elements)

            Log.d(TAG, "updateZoneNote: updatedEntry: $updatedEntry")

            val elementsMap = manager.getGrilleElements(updatedEntry)
            val zoneNote = calculateZoneNote(elements)
            val globalNote = calculateGlobalNote(elementsMap)

            Log.d(TAG, "updateZoneNote: zoneNote: $zoneNote, globalNote: $globalNote")

            _residenceData.value = updatedEntry.copy(
                prop = updatedEntry.prop?.copy(
                    ctrl = updatedEntry.prop.ctrl.copy(
                        note = globalNote
                    )
                )
            )

            manager.saveControlProgress(updatedEntry)

            _noteCtrl.value = "$globalNote%"
            updateZoneNoteUi(zoneId, zoneNote)
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
        _residenceData.value?.let { currentEntry ->
            val elementsMap = manager.getGrilleElements(currentEntry)

            val updatedZones = _btnZones.value?.map { zone ->
                elementsMap[zone.id]?.let { elements ->
                    zone.copy(note = "${calculateZoneNote(elements)}%")
                } ?: zone
            } ?: emptyList()

            _btnZones.value = updatedZones

            val globalNote = calculateGlobalNote(elementsMap)
            _noteCtrl.value = if (globalNote < 0) "S O" else "$globalNote%"

            _residenceData.value = currentEntry.copy(
                prop = currentEntry.prop?.copy(
                    ctrl = currentEntry.prop.ctrl.copy(
                        note = globalNote
                    )
                )
            )
        }
    }



    sealed class NavigationDestination {
        data object NextScreen : NavigationDestination()
        // Ajoutez d'autres destinations au besoin
    }

}
