package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConfigCtrlViewModel : ViewModel() {

    private val TAG = "ConfigCtrlViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private val _ctrlInopine = MutableLiveData(false)
    val ctrlInopine: LiveData<Boolean> get() = _ctrlInopine

    private val _meteoPerturbe = MutableLiveData(false)
    val meteoPerturbe: LiveData<Boolean> get() = _meteoPerturbe

    private val _prodPresent = MutableLiveData(true)
    val prodPresent: LiveData<Boolean> get() = _prodPresent

    private val _affConforme = MutableLiveData(true)
    val affConforme: LiveData<Boolean> get() = _affConforme



    fun setCtrlInopine(isInopine: Boolean) {
        _ctrlInopine.value = isInopine
    }
    fun setMeteoPerturbe(isPerturbe: Boolean) {
        _meteoPerturbe.value = isPerturbe
    }
    fun setProdPresent(isPresent: Boolean) {
        _prodPresent.value = isPresent
    }
    fun setAffConforme(isConforme: Boolean) {
        _affConforme.value = isConforme
    }



    fun setUserCredentials(idMbr: Int, adrMac: String) {
        this.idMbr = idMbr
        this.adrMac = adrMac

        Log.d(TAG, "setUserCredentials: idMbr: ${this.idMbr}, adrMac: ${this.adrMac}")
    }

}
