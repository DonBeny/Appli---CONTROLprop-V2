package org.orgaprop.controlprop.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.main.types.LoginData

class FinishCtrlViewModel : ViewModel() {

    private val TAG = "FinishCtrlViewModel"

    private lateinit var userData: LoginData
    private lateinit var entrySelected: SelectItem



    fun setUserData(userData: LoginData) {
        this.userData = userData
    }
    fun setEntrySelected(currentEntry: SelectItem) {
        entrySelected = currentEntry
    }

}
