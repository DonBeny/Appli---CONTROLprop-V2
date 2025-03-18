package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class SelectEntryManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    private val TAG = "SelectListManager"

    suspend fun logout(idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_CBL_LOGOUT, "", paramsPost)

            Log.d(TAG, "logout: response = $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "logout: jsonObject = $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw SelectEntryException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw SelectEntryException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw SelectEntryException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    class SelectEntryException(
        code: Int,
        cause: Throwable? = null
    ) : BaseException(code, getMessageForCode(code), cause) {

        companion object {
            /**
             * Retourne un message par défaut en fonction du code d'erreur.
             *
             * @param code Le code d'erreur.
             * @return Le message correspondant au code d'erreur.
             */
            private fun getMessageForCode(code: Int): String {
                return when (code) {
                    ErrorCodes.NETWORK_ERROR -> "Erreur réseau lors de la connexion"
                    ErrorCodes.INVALID_RESPONSE -> "Réponse serveur invalide"
                    ErrorCodes.LOGIN_FAILED -> "Échec de la connexion"
                    ErrorCodes.LOGOUT_FAILED -> "Échec de la déconnexion"
                    ErrorCodes.VERSION_CHECK_FAILED -> "Échec de la vérification de version"
                    else -> "Une erreur inconnue s'est produite"
                }
            }
        }
    }

}
