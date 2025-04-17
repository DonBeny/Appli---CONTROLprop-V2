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
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.LogUtils

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



    fun loadSavedData(entrySelected: SelectItem, zoneId: Int) {
        setEntrySelected(entrySelected)
        loadZone(zoneId)

        entrySelected.getZoneElements(zoneId)?.let { savedElements ->
            val currentElements = _elements.value?.toMutableList() ?: mutableListOf()

            LogUtils.json(TAG, "savedElements:", savedElements)
            LogUtils.json(TAG, "currentElements:", currentElements)

            savedElements.forEach { savedElement ->
                val currentElement = currentElements.find { it.id == savedElement.id }

                LogUtils.json(TAG, "savedElement", savedElement)
                LogUtils.json(TAG, "currentElement", currentElement)

                currentElement?.let { element ->
                    element.note = savedElement.note

                    LogUtils.d(TAG, "element.note => ${element.note}")

                    savedElement.criterMap.forEach { (critterId, savedCritter) ->
                        LogUtils.json(TAG, "savedCritter", savedCritter)

                        element.criterMap[critterId]?.let { currentCritter ->
                            LogUtils.json(TAG, "currentCritter", currentCritter)

                            currentCritter.note = savedCritter.note
                            currentCritter.comment = savedCritter.comment

                            LogUtils.json(TAG, "newCritter", currentCritter)
                        }
                    }
                }

                LogUtils.json(TAG, "newElement", currentElement)
            }

            _elements.value = currentElements
        }

        LogUtils.json(TAG, "elements", _elements.value)
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

            LogUtils.d(TAG, "loadZone: Loaded ${elements.size} elements for zone $zoneId")

            _isLoading.value = false
        } catch (e: BaseException) {
            _isLoading.value = false
            LogUtils.e(TAG, "Error in loadZone: ${e.message}", e)
            _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
        } catch (e: Exception) {
            _isLoading.value = false
            LogUtils.e(TAG, "Unexpected error in loadZone", e)
            _error.value = Pair(ErrorCodes.UNKNOWN_ERROR, "Erreur lors du chargement de la zone")
        }
    }

    fun updateCritterValue(elementPosition: Int, critterPosition: Int, value: Int) {
        val currentElements = _elements.value ?: return
        val updatedElements = currentElements.toMutableList()
        val element = updatedElements[elementPosition]
        val critter = element.criterMap[critterPosition] ?: return

        if (critter.note == 0) {
            critter.note = value
        } else {
            critter.note = 0
            critter.comment = ObjComment("", "")
        }

        var sumValues = 0
        var sumCoefs = 0
        var hasEvaluatedCriteria = false

        element.criterMap.values.forEach { c ->
            LogUtils.json(TAG, "updateCritterValue::c", c)

            when (c.note) {
                1 -> {
                    LogUtils.d(TAG, "updateCritterValue::c.note => 1")
                    hasEvaluatedCriteria = true
                    sumValues += c.coefProduct
                    sumCoefs += c.coefProduct
                }
                -1 -> {
                    LogUtils.d(TAG, "updateCritterValue::c.note => -1")
                    hasEvaluatedCriteria = true
                    sumCoefs += c.coefProduct
                }
            }
        }

        if (!hasEvaluatedCriteria) {
            element.note = -1
        } else {
            val meteoMajoration = configCtrl?.optString("meteo") == "true"

            if (meteoMajoration) {
                sumValues = (sumValues * 1.1).toInt()
            }

            element.note = if (sumCoefs > 0) {
                ((sumValues.toDouble() / sumCoefs) * 100).toInt()
            } else {
                0
            }
        }

        _elements.value = updatedElements
    }
    fun updateCritterComment(elementIndex: Int, critterIndex: Int, comment: String, imagePath: String) {
        val currentElements = _elements.value ?: return
        val updatedElements = currentElements.toMutableList()

        updatedElements[elementIndex].criterMap[critterIndex]?.let { critter ->
            critter.comment = ObjComment(comment, imagePath)
            _elements.value = updatedElements
        }
    }

    fun getControlledElements(): List<ObjElement> {
        return elements.value ?: emptyList()
    }

}
