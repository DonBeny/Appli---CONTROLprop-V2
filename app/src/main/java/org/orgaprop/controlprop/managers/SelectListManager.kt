package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.selectList.SelectListActivity
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class SelectListManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    private val TAG = "SelectListManager"

    suspend fun fetchData(type: String, parentId: Int, searchQuery: String, idMbr: Int, adrMac: String): JSONObject {
        val getString = "val=$parentId"
        var postString = "mbr=$idMbr&mac=$adrMac"

        if (type == SelectListActivity.SELECT_LIST_TYPE_SEARCH) {
            postString += "&search=$searchQuery"
        }

        return try {
            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_LIST, type, getString, postString)

            Log.d(TAG, "fetchData response : $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "fetchData jsonObject : $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw SelectListException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw SelectListException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw SelectListException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    // Exception personnalisée pour les erreurs de connexion
    class SelectListException(
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
