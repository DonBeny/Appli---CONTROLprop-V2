package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjStructureElement(
    val coef: Int,
    val name: String,
    val critrs: Map<String, ObjStructureCritter>
) {

    companion object {
        private const val TAG = "ObjStructureElement"

        fun fromJson(json: JSONObject): ObjStructureElement {
            try {
                return ObjStructureElement(
                    coef = json.optInt("coef", 0),
                    name = json.optString("name", ""),
                    critrs = json.optJSONObject("critrs")?.let {
                        ObjStructureCritter.parseCriters(it)
                    } ?: emptyMap()
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de l'élément de structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'élément de structure invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de l'élément de structure", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de l'élément de structure", e)
            }
        }

        fun parseElements(json: JSONObject): Map<String, ObjStructureElement> {
            try {
                val elements = mutableMapOf<String, ObjStructureElement>()
                val keys = json.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        elements[key] = fromJson(json.getJSONObject(key))
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing de l'élément de structure $key", e)
                        // On continue avec les autres éléments
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing de l'élément de structure $key", e)
                        // On continue avec les autres éléments
                    }
                }

                return elements
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing des éléments de structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Erreur lors de l'analyse des éléments de structure", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            val critrsJson = JSONObject()
            critrs.forEach { (key, critter) ->
                try {
                    critrsJson.put(key, critter.toJson())
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la sérialisation du critère $key", e)
                    // On continue avec les autres critères
                }
            }

            return JSONObject().apply {
                put("coef", coef)
                put("name", name)
                put("critrs", critrsJson)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de l'élément de structure", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de l'élément de structure en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de l'élément de structure", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de l'élément de structure", e)
        }
    }

    private fun validate() {
        if (coef < 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Coefficient d'élément invalide: $coef")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom d'élément manquant")
        }
    }

}
