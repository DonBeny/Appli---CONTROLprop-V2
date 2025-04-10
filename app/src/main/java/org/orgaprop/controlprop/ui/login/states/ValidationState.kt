package org.orgaprop.controlprop.ui.login.states

sealed class ValidationState {

    data object Valid : ValidationState()
    data class Invalid(val message: String) : ValidationState()

}
