package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjGrilleElement(
    var id: Int = 0,
    var name: String = "",
    var coef: Int = 0,
    var note: Int = 0,
    var critters: List<ObjGrilleCritter> = emptyList()
) : Serializable {

    companion object {
        private const val TAG = "ObjGrilleElement"

        fun fromJson(json: JSONObject): ObjGrilleElement {
            try {
                val critterArray = json.optJSONArray("criteria") ?: JSONArray()
                val critters = parseCritters(critterArray)

                return ObjGrilleElement(
                    id = json.optInt("id", 0),
                    name = json.optString("name", ""),
                    coef = json.optInt("coef", 0),
                    note = json.optInt("note", 0),
                    critters = critters
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de l'élément", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'élément invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de l'élément", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de l'élément", e)
            }
        }

        private fun parseCritters(jsonArray: JSONArray): List<ObjGrilleCritter> {
            val critters = mutableListOf<ObjGrilleCritter>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val critterJson = jsonArray.getJSONObject(i)
                    val critter = ObjGrilleCritter.fromJson(critterJson)
                    critters.add(critter)
                } catch (e: BaseException) {
                    Log.e(TAG, "Erreur lors du parsing du critère à l'index $i", e)
                    // On continue avec les autres critères
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inattendue lors du parsing du critère à l'index $i", e)
                    // On continue avec les autres critères
                }
            }

            return critters
        }
    }

    fun toJson(): JSONObject {
        try {
            val critterArray = JSONArray()
            critters.forEach { criterion ->
                try {
                    critterArray.put(criterion.toJson())
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la sérialisation d'un critère", e)
                    // On continue avec les autres critères
                }
            }

            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("note", note)
                put("coef", coef)
                put("critters", critterArray)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de l'élément", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de l'élément en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de l'élément", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de l'élément", e)
        }
    }

    private fun validate() {
        if (id <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID d'élément invalide: $id")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom d'élément manquant")
        }
        if (coef <= 0) {
            Log.w(TAG, "Coefficient d'élément négatif, remise à 0")
            coef = 0
        }
    }

}
