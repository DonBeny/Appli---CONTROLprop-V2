package org.orgaprop.controlprop.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

@Parcelize
data class ObjSignature(
    val controlSignature: String,
    val agentSignature: String,
    val agentName: String
) : Parcelable {

    companion object {
        fun fromJson(json: JSONObject): ObjSignature {
            try {
                return ObjSignature(
                    controlSignature = json.optString("controlSignature", ""),
                    agentSignature = json.optString("agentSignature", ""),
                    agentName = json.optString("agentName", "")
                )
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjSignature", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("controlSignature", controlSignature)
                put("agentSignature", agentSignature)
                put("agentName", agentName)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjSignature vers JSON", e)
        }
    }
}