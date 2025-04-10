package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.CtrlZoneManager
import org.orgaprop.controlprop.models.ObjComment
import org.orgaprop.controlprop.models.ObjCriter
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData

class CtrlZoneViewModel(
    private val manager: CtrlZoneManager
) : ViewModel() {

    companion object {
        const val TAG = "CtrlZoneViewModel"
    }

    private val _elements = MutableLiveData<List<ObjElement>>()
    val elements: LiveData<List<ObjElement>> = _elements

    private val _zoneName = MutableLiveData<String>()
    val zoneName: LiveData<String> = _zoneName

    private val _limits = MutableLiveData<Pair<Int, Int>>()
    val limits: LiveData<Pair<Int, Int>> = _limits

    private val _error = MutableLiveData<Pair<Int, String>?>()
    val error: LiveData<Pair<Int, String>?> = _error

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private lateinit var userData: LoginData
    private var zoneId: Int = 1
    private var configCtrl: JSONObject? = null
    private var entrySelected: SelectItem? = null



    fun setUserData(user: LoginData) {
        this.userData = user
        loadLimits()
    }
    fun setConfigCtrl(config: JSONObject) {
        this.configCtrl = config
    }
    fun setEntrySelected(entry: SelectItem) {
        this.entrySelected = entry
    }



    private fun loadLimits() {
        val max = userData.limits.top
        val min = userData.limits.down

        _limits.value = Pair(max, min)
    }

    fun getControlledElements(): List<ObjElement> {
        return elements.value ?: emptyList()
    }



    fun loadZone(zoneId: Int) {
        this.zoneId = zoneId
        _isLoading.value = true

        try {
            if (!::userData.isInitialized) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Données utilisateur non initialisées")
            }

            val entryData = entrySelected ?: throw BaseException(ErrorCodes.INVALID_DATA, "Aucune entrée sélectionnée")

            val (name, elements) = manager.loadZoneData(userData, entryData, zoneId)

            _zoneName.value = name
            _elements.value = elements

            Log.d(TAG, "loadZone: Loaded ${elements.size} elements for zone $zoneId")

            _isLoading.value = false
        } catch (e: BaseException) {
            _isLoading.value = false
            Log.e(TAG, "Error in loadZone: ${e.message}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "Unexpected error in loadZone", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Erreur lors du chargement de la zone")
        }
    }

    fun updateCritterValue(elementPosition: Int, critterPosition: Int, value: Int) {
        try {
            val currentElements = _elements.value ?: throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Aucun élément chargé")
            val meteoMajoration = configCtrl?.optString("meteo") == "true"
            val updatedElements = manager.updateCritterValue(
                currentElements,
                elementPosition,
                critterPosition,
                value,
                meteoMajoration
            )

            _elements.value = updatedElements

            Log.d(TAG, "updateCritterValue: Updated critter at element $elementPosition, position $critterPosition to $value")
        } catch (e: BaseException) {
            Log.e(TAG, "Error in updateCritterValue: ${e.message}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in updateCritterValue", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la mise à jour du critère")
        }
    }
    fun updateCritterComment(elementIndex: Int, critterIndex: Int, comment: String, imagePath: String) {
        try {
            val currentElements = _elements.value ?: throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Aucun élément chargé")
            val updatedElements = manager.updateCritterComment(
                currentElements,
                elementIndex,
                critterIndex,
                comment,
                imagePath
            )

            _elements.value = updatedElements

            Log.d(TAG, "updateCritterComment: Updated comment for element $elementIndex, critter $critterIndex")
        } catch (e: BaseException) {
            Log.e(TAG, "Error in updateCritterComment: ${e.message}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in updateCritterComment", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la mise à jour du commentaire")
        }
    }



    fun clearError() {
        _error.value = null
    }


}
