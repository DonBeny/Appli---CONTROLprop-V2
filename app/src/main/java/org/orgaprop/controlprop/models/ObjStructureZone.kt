package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjStructureZone(
    val coef: Int,
    val name: String,
    val elmts: Map<String, ObjStructureElement>
) {

    companion object {
        private const val TAG = "ObjStructureZone"

        fun fromJson(json: JSONObject): ObjStructureZone {
            try {
                return ObjStructureZone(
                    coef = json.optInt("coef", 0),
                    name = json.optString("name", ""),
                    elmts = json.optJSONObject("elmts")?.let {
                        ObjStructureElement.parseElements(it)
                    } ?: emptyMap()
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la zone de structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de zone de structure invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la zone de structure", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la zone de structure", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            val elmtsJson = JSONObject()
            elmts.forEach { (key, element) ->
                try {
                    elmtsJson.put(key, element.toJson())
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la sérialisation de l'élément $key", e)
                    // On continue avec les autres éléments
                }
            }

            return JSONObject().apply {
                put("coef", coef)
                put("name", name)
                put("elmts", elmtsJson)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de la zone de structure", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de la zone de structure en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de la zone de structure", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de la zone de structure", e)
        }
    }

    private fun validate() {
        if (coef < 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Coefficient de zone invalide: $coef")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom de zone manquant")
        }
    }

}
