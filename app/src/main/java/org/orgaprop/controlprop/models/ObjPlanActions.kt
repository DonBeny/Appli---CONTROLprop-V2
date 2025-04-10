package org.orgaprop.controlprop.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

@Parcelize
data class ObjPlanActions(
    val id: Int = 0,
    val limit: String,
    val txt: String,
    val isLevee: Boolean = false
) : Parcelable {

    companion object {
        fun fromJson(json: JSONObject): ObjPlanActions {
            try {
                return ObjPlanActions(
                    id = json.optInt("id", 0),
                    limit = json.optString("limit", ""),
                    txt = json.optString("txt", ""),
                    isLevee = json.optBoolean("isLevee", false)
                )
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjPlanActions", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("id", id)
                put("limit", limit)
                put("txt", txt)
                put("isLevee", isLevee)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjPlanActions vers JSON", e)
        }
    }

}
