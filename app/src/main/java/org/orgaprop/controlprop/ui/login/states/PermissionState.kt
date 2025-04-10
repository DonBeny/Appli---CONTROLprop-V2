package org.orgaprop.controlprop.ui.login.states

sealed class PermissionState {

    data object Granted : PermissionState()
    data class Denied(val message: String) : PermissionState()
    data class Error(val message: String) : PermissionState()

}
