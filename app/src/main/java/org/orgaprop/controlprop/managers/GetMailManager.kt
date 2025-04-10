package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class GetMailManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    companion object {
        private const val TAG = "GetMailManager"
    }



    /**
     * Envoie une requête au serveur pour récupérer les identifiants via email.
     *
     * @param email L'adresse email de l'utilisateur
     * @return Le JSONObject contenant la réponse du serveur
     * @throws BaseException Si une erreur survient lors de la communication avec le serveur
     */
    suspend fun submitEmail(email: String): JSONObject = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Envoi de la demande de récupération d'identifiants pour: $email")
            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION,
                HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_MAIL,
                "",
                "email=$email"
            )

            val responseJson = JSONObject(response)

            if (responseJson.has("status")) {
                if (responseJson.getBoolean("status")) {
                    Log.d(TAG, "Récupération des identifiants réussie")
                } else {
                    Log.w(TAG, "Échec de la récupération des identifiants: ${responseJson.optString("message", "Raison inconnue")}")
                }
            } else {
                Log.w(TAG, "Réponse inattendue du serveur (pas de statut)")
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "La réponse du serveur ne contient pas de statut")
            }

            return@withContext responseJson
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des identifiants", e)
            when (e) {
                is BaseException -> throw e
                else -> throw BaseException(ErrorCodes.NETWORK_ERROR, "Erreur de communication avec le serveur", e)
            }
        }
    }

}
