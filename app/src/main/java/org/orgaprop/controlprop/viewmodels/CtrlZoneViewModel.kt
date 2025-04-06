package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import org.orgaprop.controlprop.models.ObjComment
import org.orgaprop.controlprop.models.ObjCriter
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.main.types.LoginData

class CtrlZoneViewModel : ViewModel() {

    private val TAG = "CtrlZoneViewModel"

    private val _elements = MutableLiveData<List<ObjElement>>()
    val elements: LiveData<List<ObjElement>> = _elements

    private val _zoneName = MutableLiveData<String>()
    val zoneName: LiveData<String> = _zoneName

    private val _limits = MutableLiveData<Pair<Int, Int>>()
    val limits: LiveData<Pair<Int, Int>> = _limits

    private lateinit var userData: LoginData
    private var zoneCoef: Int = 1
    private var configCtrl: JSONObject? = null



    fun setUserData(user: LoginData) {
        this.userData = user
        loadLimits()
    }
    fun setConfigCtrl(config: JSONObject) {
        this.configCtrl = config
    }



    private fun loadLimits() {
        val max = userData.limits.top
        val min = userData.limits.down

        _limits.value = Pair(max, min)
    }



    fun loadSavedData(entrySelected: SelectItem, zoneId: Int) {
        val myTag = "$TAG::loadSavedData"
        loadZone(zoneId)

        entrySelected.getZoneElements(zoneId)?.let { savedElements ->
            val currentElements = _elements.value?.toMutableList() ?: mutableListOf()

            Log.d(myTag, "savedElements => $savedElements")
            Log.d(myTag, "currentElements => $currentElements")

            savedElements.forEach { savedElement ->
                val currentElement = currentElements.find { it.id == savedElement.id }

                Log.d(myTag, "savedElement => $savedElement")
                Log.d(myTag, "currentElement => $currentElement")

                currentElement?.let { element ->
                    element.note = savedElement.note

                    Log.d(myTag, "element.note => ${element.note}")

                    savedElement.criterMap.forEach { (critterId, savedCritter) ->
                        Log.d(myTag, "savedCritter => $savedCritter")

                        element.criterMap[critterId]?.let { currentCritter ->
                            Log.d(myTag, "currentCritter => $currentCritter")

                            currentCritter.note = savedCritter.note
                            currentCritter.comment = savedCritter.comment

                            Log.d(myTag, "newCritter => $currentCritter")
                        }
                    }
                }

                Log.d(myTag, "newElement => $currentElement")
            }

            _elements.value = currentElements
        }

        Log.d(myTag, "elements => ${_elements.value}")
    }
    fun loadZone(zoneId: Int) {
        val myTag = "$TAG::loadZone"
        val structureZone = userData.structure[zoneId.toString()] ?: return

        Log.d(myTag, "StructureZone => $structureZone")

        _zoneName.value = structureZone.name

        _elements.value = structureZone.elmts.mapNotNull { (elementId, element) ->
            if (element.coef > 0) {
                ObjElement(
                    id = elementId.toInt(),
                    coef = element.coef
                ).apply {
                    element.critrs.forEach { (critterId, critter) ->
                        if (critter.coef > 0) {
                            this.addCriter(ObjCriter(
                                id = critterId.toInt(),
                                coefProduct = zoneCoef * element.coef * critter.coef
                            ))
                        }
                    }
                }.takeIf { it.criterMap.isNotEmpty() }
            } else null
        }

        Log.d(myTag, "Elements => ${_elements.value}")
    }

    fun updateCritterValue(elementPosition: Int, critterPosition: Int, value: Int) {
        val currentElements = _elements.value ?: return
        val updatedElements = currentElements.toMutableList()
        val element = updatedElements[elementPosition]
        val critter = element.criterMap[critterPosition] ?: return

        // Mise à jour de la valeur du critère
        critter.note = value

        // Calcul des sommes
        var sumValues = 0
        var sumCoefs = 0

        element.criterMap.values.forEach { c ->
            Log.d(TAG, "updateCritterValue::c => $c")

            when (c.note) {
                1 -> {
                    Log.d(TAG, "updateCritterValue::c.note => 1")
                    sumValues += c.coefProduct
                    sumCoefs += c.coefProduct
                }
                -1 -> {
                    Log.d(TAG, "updateCritterValue::c.note => -1")
                    sumCoefs += c.coefProduct
                    c.comment = ObjComment("", "")
                }
            }
        }

        // Application de la majoration météo
        val meteoMajoration = configCtrl?.optString("meteo") == "true"
        if (meteoMajoration) {
            sumValues = (sumValues * 1.1).toInt()
        }

        // Calcul final de la note
        element.note = if (sumCoefs > 0) {
            ((sumValues.toDouble() / sumCoefs) * 100).toInt()
        } else {
            0
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
