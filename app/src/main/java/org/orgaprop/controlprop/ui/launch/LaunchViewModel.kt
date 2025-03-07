package org.orgaprop.controlprop.ui.launch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LaunchViewModel : ViewModel() {

    private val _navigationEvent = MutableLiveData<Unit>()
    val navigationEvent: LiveData<Unit> get() = _navigationEvent

    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> get() = _errorEvent

    fun delayedNavigateToMain() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(LaunchActivityConfig.SPLASH_SCREEN_DELAY)
            navigateToMain()
        }
    }

    fun navigateToMain() {
        _navigationEvent.value = Unit
    }

    fun showError(message: String) {
        _errorEvent.value = message
    }

}
