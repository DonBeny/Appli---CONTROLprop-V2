package org.orgaprop.controlprop.models

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

@Parcelize
data class ObjConfig(
    var visite: Boolean = false,
    var meteo: Boolean = false,
    var affichage: Boolean = false,
    var produits: Boolean = false
) : Parcelable, Serializable {

    companion object {
        private const val TAG = "ObjConfig"

        fun fromJson(json: JSONObject): ObjConfig {
            try {
                return ObjConfig(
                    visite = json.optBoolean("visite", false),
                    meteo = json.optBoolean("meteo", false),
                    affichage = json.optBoolean("aff", false),
                    produits = json.optBoolean("prod", false)
                )
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la configuration", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de configuration invalide", e)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la configuration", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la configuration", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("visite", visite)
                put("meteo", meteo)
                put("aff", affichage)
                put("prod", produits)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de la configuration", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de la configuration en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de la configuration", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de la configuration", e)
        }
    }

}
