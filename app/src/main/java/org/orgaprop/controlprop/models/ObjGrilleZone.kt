package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjGrilleZone(
    var zoneId: Int = 0,
    var elements: List<ObjGrilleElement> = emptyList()
) : Serializable {
    companion object {
        private const val TAG = "ObjGrilleZone"

        fun fromJson(json: JSONObject): ObjGrilleZone {
            try {
                val elementsArray = json.optJSONArray("elements") ?: JSONArray()
                val elements = parseElements(elementsArray)

                return ObjGrilleZone(
                    zoneId = json.optInt("zoneId", 0),
                    elements = elements
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la zone", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de zone invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la zone", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la zone", e)
            }
        }

        private fun parseElements(jsonArray: JSONArray): List<ObjGrilleElement> {
            val elements = mutableListOf<ObjGrilleElement>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val elementJson = jsonArray.getJSONObject(i)
                    val element = ObjGrilleElement.fromJson(elementJson)
                    elements.add(element)
                } catch (e: BaseException) {
                    Log.e(TAG, "Erreur lors du parsing de l'élément à l'index $i", e)
                    // On continue avec les autres éléments
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inattendue lors du parsing de l'élément à l'index $i", e)
                    // On continue avec les autres éléments
                }
            }

            return elements
        }
    }

    fun toJson(): JSONObject {
        try {
            val elementsArray = JSONArray()
            elements.forEach { element ->
                try {
                    elementsArray.put(element.toJson())
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la sérialisation d'un élément", e)
                    // On continue avec les autres éléments
                }
            }

            return JSONObject().apply {
                put("zoneId", zoneId)
                put("elements", elementsArray)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de la zone", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de la zone en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de la zone", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de la zone", e)
        }
    }

    private fun validate() {
        if (zoneId <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID de zone invalide: $zoneId")
        }
    }

}
