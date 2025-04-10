package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjInfo(
    val aff: String,
    val prod: String
) {

    companion object {
        private const val TAG = "ObjInfo"

        fun fromJson(json: JSONObject): ObjInfo {
            try {
                return ObjInfo(
                    aff = json.optString("aff", ""),
                    prod = json.optString("prod", "")
                )
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing des informations", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'informations invalide", e)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing des informations", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse des informations", e)
            }
        }
    }



    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("aff", aff)
                put("prod", prod)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation des informations", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion des informations en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation des informations", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion des informations", e)
        }
    }

}
