package org.orgaprop.controlprop.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.managers.GetMailManager
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class GetMailViewModel(private val getMailManager: GetMailManager, private val networkMonitor: NetworkMonitor) : ViewModel() {

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> get() = _response

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun submitEmail(email: String) {
        viewModelScope.launch {
            try {
                val result = getMailManager.submitEmail(email)

                if (result.has("status") && result.getBoolean("status")) {
                    _response.value = "Votre avez été déconnecté et vos identifiants vous ont été envoyés"
                } else {
                    _response.value = result.getString("message")
                    _error.value = result.getString("error")
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

}
