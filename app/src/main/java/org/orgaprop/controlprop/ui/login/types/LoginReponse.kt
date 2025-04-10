package org.orgaprop.controlprop.ui.login.types

import org.json.JSONObject
import org.orgaprop.controlprop.models.LoginData

data class LoginResponse(
    val status: Boolean,
    val data: LoginData? = null,
    val error: LoginError? = null
) {
    companion object {
        fun fromJson(json: JSONObject): LoginResponse {
            val status = json.getBoolean("status")

            return LoginResponse(
                status = status,
                data = if (status && json.has("data")) LoginData.fromJson(json.getJSONObject("data")) else null,
                error = if (!status && json.has("error")) LoginError.fromJson(json.getJSONObject("error")) else null
            )
        }
    }
}



data class LoginError(
    val code: Int,
    val txt: String
) {
    companion object {
        fun fromJson(json: JSONObject): LoginError {
            return LoginError(
                code = json.getInt("code"),
                txt = json.getString("txt")
            )
        }
    }
}
