package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.managers.TypeCtrlManager

class TypeCtrlViewModel(private val typeCtrlManager: TypeCtrlManager) : ViewModel() {

    private val TAG = "TypeCtrlViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private val _planActionResult = MutableLiveData<Boolean>()
    val planActionResult: LiveData<Boolean> get() = _planActionResult

    fun fetchPlanAction(rsd: Int) {
        viewModelScope.launch {
            try {
                val response = typeCtrlManager.fetchPlanAction(rsd, idMbr, adrMac)

                val id = response.optInt("id", -1)
                _planActionResult.value = id > 0
            } catch (e: TypeCtrlManager.TypeCtrlException) {
                _planActionResult.value = false
                Log.e(TAG, "fetchPlanAction: Error fetching plan action", e)
            }
        }
    }

    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        Log.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
    }

}
