package org.orgaprop.controlprop.ui.login.states

sealed class LogoutState {

    data object Loading : LogoutState()
    data object Success : LogoutState()
    data class Error(val message: String) : LogoutState()

}
