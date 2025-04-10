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
    var note: Int = 0
) : Serializable {
    companion object {
        private const val TAG = "ObjGrilleCritter"

        fun fromJson(json: JSONObject): ObjGrilleCritter {
            try {
                return ObjGrilleCritter(
                    id = json.optInt("id", 0),
                    name = json.optString("name", ""),
                    note = json.optInt("note", 0)
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
                put("note", note)
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
        if (note < 0) {
            Log.w(TAG, "Note de critère négative, remise à 0")
            note = 0
        }
    }
}
