package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjStructureCritter(
    val coef: Int,
    val name: String
) {

    companion object {
        private const val TAG = "ObjStructureCritter"

        fun fromJson(json: JSONObject): ObjStructureCritter {
            try {
                return ObjStructureCritter(
                    coef = json.optInt("coef", 0),
                    name = json.optString("name", "")
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing du critère de structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de critère de structure invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing du critère de structure", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du critère de structure", e)
            }
        }

        fun parseCriters(json: JSONObject): Map<String, ObjStructureCritter> {
            try {
                val criters = mutableMapOf<String, ObjStructureCritter>()
                val keys = json.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        criters[key] = fromJson(json.getJSONObject(key))
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing du critère de structure $key", e)
                        // On continue avec les autres critères
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing du critère de structure $key", e)
                        // On continue avec les autres critères
                    }
                }

                return criters
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing des critères de structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Erreur lors de l'analyse des critères de structure", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("coef", coef)
                put("name", name)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation du critère de structure", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion du critère de structure en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation du critère de structure", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion du critère de structure", e)
        }
    }

    private fun validate() {
        if (coef < 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Coefficient de critère invalide: $coef")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom de critère manquant")
        }
    }

}
