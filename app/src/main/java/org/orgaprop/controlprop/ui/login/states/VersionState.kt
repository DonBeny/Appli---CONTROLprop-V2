package org.orgaprop.controlprop.ui.login.states

import org.json.JSONObject

sealed class VersionState {

    data object Loading : VersionState()
    data class Success(val response: JSONObject) : VersionState()
    data class Error(val message: String) : VersionState()

}
