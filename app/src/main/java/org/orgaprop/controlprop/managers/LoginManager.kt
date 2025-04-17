package org.orgaprop.controlprop.managers

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class LoginManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    private val TAG = "LoginManager"

    /**
     * Effectue la connexion de l'utilisateur
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun login(username: String, password: String): JSONObject {
        return executeWithExceptionHandling {
            // Récupérer le numéro de version de l'application
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName

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

            LogUtils.json(TAG, "login paramsPost:", paramsPost)

            // Exécuter la requête HTTP
            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION,
                HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_LOGIN,
                "",
                paramsPost
            )

            LogUtils.json(TAG, "login response:", response)

            JSONObject(response)
        }
    }

    /**
     * Vérifie les identifiants de connexion sans réellement se connecter
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun checkLogin(username: String, password: String, adrMac: String): JSONObject {
        return executeWithExceptionHandling {
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

            LogUtils.json(TAG, "checkLogin: response", response)

            JSONObject(response)
        }
    }

    /**
     * Déconnecte l'utilisateur
     *
     * @param idMbr ID du membre
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun logout(idMbr: Int, adrMac: String): JSONObject {
        return executeWithExceptionHandling {
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_LOGOUT, "", paramsPost)

            LogUtils.json(TAG, "logout: response", response)

            JSONObject(response)
        }
    }

    /**
     * Efface les données de connexion stockées localement
     */
    fun clearLoginData() {
        // TODO: Implémenter la logique pour effacer les données de connexion
        // Par exemple, supprimer les données dans les SharedPreferences
    }

    /**
     * Méthode helper pour exécuter un bloc de code avec une gestion d'exceptions standard
     *
     * @param block Bloc de code à exécuter
     * @return Le résultat du bloc de code
     * @throws LoginException Si une erreur survient pendant l'exécution
     */
    private suspend fun <T> executeWithExceptionHandling(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: IOException) {
            LogUtils.e(TAG, "Erreur réseau: ${e.message}", e)
            throw LoginException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            LogUtils.e(TAG, "Réponse JSON invalide: ${e.message}", e)
            throw LoginException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Erreur inconnue: ${e.message}", e)
            throw LoginException(ErrorCodes.UNKNOWN_ERROR, e)
        }
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