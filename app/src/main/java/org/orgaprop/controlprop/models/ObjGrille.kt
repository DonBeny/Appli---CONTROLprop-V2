package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjGrille(
    var zones: List<ObjGrilleZone> = emptyList()
) : Serializable {

    companion object {
        private const val TAG = "ObjGrille"

        fun fromJson(json: JSONObject): ObjGrille {
            try {
                val zonesArray = json.optJSONArray("zones") ?: JSONArray()
                val zones = parseZones(zonesArray)

                return ObjGrille(zones)
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la grille", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de grille invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la grille", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la grille", e)
            }
        }

        fun fromJsonArray(jsonArray: JSONArray): ObjGrille {
            try {
                val zones = parseZones(jsonArray)
                return ObjGrille(zones)
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing du tableau de grille", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de tableau de grille invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing du tableau de grille", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du tableau de grille", e)
            }
        }

        private fun parseZones(jsonArray: JSONArray): List<ObjGrilleZone> {
            val zones = mutableListOf<ObjGrilleZone>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val zoneJson = jsonArray.getJSONObject(i)
                    val zone = ObjGrilleZone.fromJson(zoneJson)
                    zones.add(zone)
                } catch (e: BaseException) {
                    Log.e(TAG, "Erreur lors du parsing de la zone à l'index $i", e)
                    // On continue avec les autres zones
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inattendue lors du parsing de la zone à l'index $i", e)
                    // On continue avec les autres zones
                }
            }

            return zones
        }
    }



    fun toJsonArray(): JSONArray {
        try {
            val jsonArray = JSONArray()
            zones.forEach { zone ->
                try {
                    jsonArray.put(zone.toJson())
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la sérialisation d'une zone", e)
                    // On continue avec les autres zones
                }
            }
            return jsonArray
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sérialisation de la grille", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de la grille", e)
        }
    }

}
