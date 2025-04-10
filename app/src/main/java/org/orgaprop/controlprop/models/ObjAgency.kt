package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

data class ObjAgency(
    val id: Int,
    val nom: String,
    val tech: String,
    val contact: String
) {

    companion object {
        private const val TAG = "ObjAgency"

        fun fromJson(json: JSONObject): ObjAgency {
            try {
                return ObjAgency(
                    id = json.optInt("id", 0),
                    nom = json.optString("nom", ""),
                    tech = json.optString("tech", ""),
                    contact = json.optString("contact", "")
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de l'agence", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'agence invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de l'agence", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de l'agence", e)
            }
        }

        fun parseAgencies(jsonArray: JSONArray): List<ObjAgency> {
            try {
                val agencies = mutableListOf<ObjAgency>()

                for (i in 0 until jsonArray.length()) {
                    try {
                        val agency = fromJson(jsonArray.getJSONObject(i))
                        agencies.add(agency)
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing de l'agence à l'index $i", e)
                        // On continue avec les autres agences même si celle-ci a échoué
                    }
                }

                if (agencies.isEmpty()) {
                    Log.w(TAG, "Aucune agence n'a pu être analysée correctement")
                }

                return agencies
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing des agences", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Erreur lors de l'analyse des agences", e)
            }
        }
    }

    private fun validate() {
        if (id <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID d'agence invalide: $id")
        }
        if (nom.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom d'agence manquant")
        }
    }

}
