package org.orgaprop.controlprop.models

import org.json.JSONException

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

@Parcelize
data class ObjAgent(
    var id: Int = 0,
    var name: String = "",
    var type: String = "",
) : Parcelable {

    companion object {
        private const val TAG = "ObjAgent"

        fun fromJson(json: JSONObject): ObjAgent {
            try {
                return ObjAgent(
                    id = json.optInt("id", 0),
                    name = json.optString("txt", ""),
                    type = json.optString("type", "agent")
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de l'agent", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'agent invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de l'agent", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de l'agent", e)
            }
        }

        fun parseListFromJson(json: JSONObject): List<ObjAgent> {
            try {
                val list = mutableListOf<ObjAgent>()
                val keys = json.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        val jsonObject = json.getJSONObject(key)
                        val objAgent = ObjAgent(
                            id = jsonObject.optInt("id", 0),
                            name = jsonObject.optString("name", ""),
                            type = jsonObject.optString("type", "agent")
                        ).also { it.validate() }
                        list.add(objAgent)
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing de l'agent $key", e)
                        // On continue avec les autres agents même si celui-ci a échoué
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing de l'agent $key", e)
                        // On continue avec les autres agents même si celui-ci a échoué
                    }
                }

                if (list.isEmpty()) {
                    Log.w(TAG, "Aucun agent n'a pu être analysé correctement")
                }

                return list
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing de la liste d'agents", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Erreur lors de l'analyse de la liste d'agents", e)
            }
        }
    }

    private fun validate() {
        if (id <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID d'agent invalide: $id")
        }
        if (name.isBlank()) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Nom d'agent manquant")
        }
    }

}

fun JSONObject.toObjAgentList(): List<ObjAgent> {
    return try {
        ObjAgent.parseListFromJson(this)
    } catch (e: BaseException) {
        Log.e("JSONObject", "Erreur lors de la conversion en liste d'agents", e)
        emptyList() // Retourner une liste vide en cas d'erreur
    } catch (e: Exception) {
        Log.e("JSONObject", "Erreur inattendue lors de la conversion en liste d'agents", e)
        throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion en liste d'agents", e)
    }
}
