package org.orgaprop.controlprop.models

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.ObjAgency.Companion.parseAgencies

data class LoginData(
    val agencies: List<ObjAgency>,
    val version: Int,
    val idMbr: Int,
    val adrMac: String,
    val mail: String,
    val hasContract: Boolean,
    val info: ObjInfo,
    val limits: ObjLimits,
    val planActions: String,
    val structure: Map<String, ObjStructureZone>
) {
    companion object {
        private const val TAG = "LoginData"

        fun fromJson(json: JSONObject): LoginData {
            try {
                return LoginData(
                    agencies = parseAgencies(json.getJSONArray("agences")),
                    version = json.optInt("version", 0),
                    idMbr = json.optInt("idMbr", -1),
                    adrMac = json.optString("adrMac", ""),
                    mail = json.optString("mail", ""),
                    hasContract = json.optBoolean("hasContrat", false),
                    info = ObjInfo.fromJson(json.getJSONObject("info")),
                    limits = ObjLimits.fromJson(json.getJSONObject("limits")),
                    planActions = json.optString("planActions", ""),
                    structure = parseStructure(json.getJSONObject("structure"))
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de LoginData", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de réponse de connexion invalide", e)
            } catch (e: BaseException) {
                throw e // Laisser passer les BaseException déjà créées
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de LoginData", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse des données utilisateur", e)
            }
        }

        private fun parseStructure(json: JSONObject): Map<String, ObjStructureZone> {
            try {
                val structure = mutableMapOf<String, ObjStructureZone>()
                val keys = json.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        structure[key] = ObjStructureZone.fromJson(json.getJSONObject(key))
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du parsing de la zone $key", e)
                        // On continue avec les autres zones même si celle-ci a échoué
                    }
                }

                return structure
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la structure", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de structure invalide", e)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la structure", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la structure", e)
            }
        }

    }

    private fun validate() {
        if (idMbr <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID membre invalide: $idMbr")
        }
        if (adrMac.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Adresse MAC manquante")
        }
    }

}
