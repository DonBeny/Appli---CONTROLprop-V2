package org.orgaprop.controlprop.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch

import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.TypeCtrlManager
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.config.managers.RandomGeneratorManager
import org.orgaprop.controlprop.utils.LogUtils

class TypeCtrlViewModel(private val typeCtrlManager: TypeCtrlManager) : ViewModel() {

    companion object {
        private const val TAG = "TypeCtrlViewModel"
    }

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private val _planActionResult = MutableLiveData<Boolean>()
    val planActionResult: LiveData<Boolean> get() = _planActionResult

    private val _randomGenerationCompleted = MutableLiveData<Boolean>()
    val randomGenerationCompleted: LiveData<Boolean> get() = _randomGenerationCompleted

    private val _randomControlList = MutableLiveData<List<SelectItem>>()

    private val _error = MutableLiveData<Pair<Int, String>?>(null)
    val error: LiveData<Pair<Int, String>?> get() = _error



    fun fetchPlanAction(rsd: Int) {
        viewModelScope.launch {
            try {
                val response = typeCtrlManager.fetchPlanAction(rsd, idMbr, adrMac)

                val id = response.optJSONObject("data")?.optInt("id", -1)
                _planActionResult.value = (id != null &&  id > 0)
            } catch (e: TypeCtrlManager.TypeCtrlException) {
                _planActionResult.value = false
                _error.value = Pair(e.code, e.message ?: ErrorCodes.getMessageForCode(e.code))
                LogUtils.e(TAG, "fetchPlanAction: Error fetching plan action", e)
            }
        }
    }



    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        LogUtils.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
    }



    /**
     * Génère un contrôle aléatoire pour l'entrée sélectionnée
     */
    fun generateRandomControl(
        selectedEntry: SelectItem,
        entryList: List<SelectItem>,
        useProxi: Boolean,
        useContract: Boolean
    ) {
        viewModelScope.launch {
            try {
                LogUtils.json(TAG, "Génération de contrôle aléatoire pour", selectedEntry)

                val randomControls = RandomGeneratorManager.generateRandomControl(
                    selectedEntry,
                    entryList,
                    useProxi,
                    useContract
                )

                LogUtils.json(TAG, "Contrôle aléatoire généré", randomControls)

                if (randomControls.isEmpty()) {
                    _error.value = Pair(
                        ErrorCodes.DATA_NOT_FOUND,
                        "Aucune zone disponible pour le contrôle aléatoire"
                    )
                    _randomGenerationCompleted.value = false
                } else {
                    _randomControlList.value = randomControls
                    _randomGenerationCompleted.value = true
                }

            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur lors de la génération du contrôle aléatoire", e)
                _error.value = Pair(
                    ErrorCodes.UNKNOWN_ERROR,
                    "Erreur lors de la génération du contrôle aléatoire"
                )
                _randomGenerationCompleted.value = false
            }
        }
    }

    /**
     * Retourne la liste des contrôles aléatoires générés
     */
    fun getRandomControlList(): List<SelectItem> {
        return _randomControlList.value ?: emptyList()
    }

}
