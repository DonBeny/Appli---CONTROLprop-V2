package org.orgaprop.controlprop.ui.login.states

import org.orgaprop.controlprop.models.LoginData

sealed class LoginState {

    data object Loading : LoginState()
    data class Success(val data: LoginData) : LoginState()
    data object LoggedOut : LoginState()
    data class Error(val message: String) : LoginState()

}
