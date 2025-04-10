package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjBtnZone(
    val id: Int,
    val txt: String,
    val note: String,
    val icon: Int,
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): ObjBtnZone {
            try {
                return ObjBtnZone(
                    id = json.optInt("id", 0),
                    txt = json.optString("txt", ""),
                    note = json.optString("note", ""),
                    icon = json.optInt("icon", 0)
                )
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjBtnZone", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("id", id)
                put("txt", txt)
                put("note", note)
                put("icon", icon)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjBtnZone vers JSON", e)
        }
    }

}
