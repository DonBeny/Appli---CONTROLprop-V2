package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjGrilleCritter(
    var id: Int = 0,
    var name: String = "",
    var coef: Int = 0,
    var note: Int = 0,
    var comment: ObjComment,
) : Serializable {
    companion object {
        private const val TAG = "ObjGrilleCritter"

        fun fromJson(json: JSONObject): ObjGrilleCritter {
            try {
                return ObjGrilleCritter(
                    id = json.optInt("id", 0),
                    name = json.optString("name", ""),
                    coef = json.optInt("coef", 0),
                    note = json.optInt("note", 0),
                    comment = ObjComment.fromJson(json.getJSONObject("comment"))
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing du critère", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de critère invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing du critère", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du critère", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("coef", coef)
                put("note", note)
                put("comment", comment.toJson())
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation du critère", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion du critère en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation du critère", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion du critère", e)
        }
    }

    private fun validate() {
        if (id <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID de critère invalide: $id")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom de critère manquant")
        }
        if (coef < 0) {
            Log.w(TAG, "Note de critère négative, remise à 0")
            coef = 0
        }
    }
}
