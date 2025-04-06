package org.orgaprop.controlprop.managers

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class LoginManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    private val TAG = "LoginManager"

    suspend fun login(username: String, password: String): JSONObject {
        return try {
            // Récupérer le numéro de version de l'application
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName // ou packageInfo.versionCode pour le code de version
            // Construire les paramètres pour la requête
            val paramsPost = JSONObject().apply {
                put("psd", username)
                put("mdp", password)
                put("mac", Build.FINGERPRINT)
                put("phoneName", Build.BRAND)
                put("phoneModel", Build.DEVICE)
                put("phoneBuild", Build.VERSION.SDK_INT.toString())
                put("appVersion", appVersion)
            }.toString()

            Log.d(TAG, "login paramsPost : $paramsPost")

            // Exécuter la requête HTTP
            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_LOGIN, "", paramsPost)

            Log.d(TAG, "login response : $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "login jsonObject : $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw LoginException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw LoginException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw LoginException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    suspend fun checkLogin(username: String, password: String, adrMac: String): JSONObject {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName
            val paramsPost = JSONObject().apply {
                put("psd", username)
                put("mdp", password)
                put("mac", adrMac)
                put("phoneName", Build.BRAND)
                put("phoneModel", Build.DEVICE)
                put("phoneBuild", Build.VERSION.SDK_INT.toString())
                put("appVersion", appVersion)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_TEST, "", paramsPost)

            Log.d(TAG, "checkLogin: response : $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "checkLogin: jsonObject : $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw LoginException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw LoginException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw LoginException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    suspend fun logout(idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_LOGOUT, "", paramsPost)
            val jsonObject = JSONObject(response)

            Log.d(TAG, "logout: $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw LoginException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw LoginException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw LoginException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    fun clearLoginData() {
        // Logique pour effacer les données de connexion
    }

    class LoginException(
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
