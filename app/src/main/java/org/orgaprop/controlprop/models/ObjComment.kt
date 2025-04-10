package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjComment(
    var txt: String = "",
    var img: String = ""
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): ObjComment {
            try {
                return ObjComment(
                    txt = json.optString("txt", ""),
                    img = json.optString("img", "")
                )
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjComment", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("txt", txt)
                put("img", img)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjComment vers JSON", e)
        }
    }

}
