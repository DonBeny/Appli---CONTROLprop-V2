package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjLimits(
    val top: Int,
    val down: Int,
    val rapport: ObjRapport,
    val autoPlan: Int,
) {

    companion object {
        private const val TAG = "ObjLimits"

        fun fromJson(json: JSONObject): ObjLimits {
            try {
                return ObjLimits(
                    top = json.optInt("top", 0),
                    down = json.optInt("down", 0),
                    rapport = ObjRapport.fromJson(json.optJSONObject("rapport") ?: JSONObject()),
                    autoPlan = json.optInt("auto", 0)
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing des limites", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de limites invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing des limites", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse des limites", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("top", top)
                put("down", down)
                put("rapport", rapport.toJson())
                put("auto", autoPlan)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation des limites", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion des limites en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation des limites", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion des limites", e)
        }
    }

    private fun validate() {
        if (top < 0 || down < 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Limites invalides: top=$top, down=$down")
        }
    }

}

data class ObjRapport(
    val value: Int,
    val dest: String
) {

    companion object {
        private const val TAG = "Rapport"

        fun fromJson(json: JSONObject): ObjRapport {
            try {
                return ObjRapport(
                    value = json.optInt("value", 0),
                    dest = json.optString("dest", "")
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing du rapport", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de rapport invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing du rapport", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du rapport", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("value", value)
                put("dest", dest)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation du rapport", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion du rapport en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation du rapport", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion du rapport", e)
        }
    }

    private fun validate() {
        if (value < 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Valeur de rapport invalide: $value")
        }
    }

}
