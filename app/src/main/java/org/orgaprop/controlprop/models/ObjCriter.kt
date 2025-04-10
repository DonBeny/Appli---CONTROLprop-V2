package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjCriter(
    var id: Int = 0,
    var note: Int = 0,
    var coefProduct: Int = 0,
    var comment: ObjComment = ObjComment()
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): ObjCriter {
            try {
                val objCriter = ObjCriter(
                    id = json.optInt("id", 0),
                    note = json.optInt("note", 0),
                    coefProduct = json.optInt("coefProduct", 0)
                )

                json.optJSONObject("comment")?.let {
                    objCriter.comment = ObjComment.fromJson(it)
                }

                return objCriter
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjCriter", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("id", id)
                put("note", note)
                put("coefProduct", coefProduct)
                put("comment", comment.toJson())
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjCriter vers JSON", e)
        }
    }

}
