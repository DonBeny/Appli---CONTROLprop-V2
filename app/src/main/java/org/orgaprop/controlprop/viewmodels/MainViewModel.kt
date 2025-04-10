package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

import org.orgaprop.controlprop.ui.main.inteface.MainEvent

class MainViewModel : ViewModel() {

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events



    fun navigateToMain() {
        viewModelScope.launch {
            try {
                _events.emit(MainEvent.NavigateToMain)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to emit navigation event", e)
            }
        }
    }

    fun showError(message: String) {
        viewModelScope.launch {
            _events.emit(MainEvent.ShowError(message))
        }
    }

}
