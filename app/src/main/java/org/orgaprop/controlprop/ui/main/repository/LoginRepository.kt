package org.orgaprop.controlprop.ui.main.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.test7.security.auth.LoginInterfaces

class LoginRepository(context: Context) {

    private val loginManager: LoginManager = LoginManager.getInstance(context)

    /**
     * Tente de connecter un utilisateur.
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @param remember Si true, sauvegarde les credentials
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun login(username: String, password: String, remember: Boolean): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                loginManager.login(username, password, remember).get()
            } catch (e: Exception) {
                throw LoginException("Login failed: ${e.message}", e)
            }
        }
    }

    /**
     * Tente de déconnecter un utilisateur.
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun logout(username: String, password: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                loginManager.logout(username, password).get()
            } catch (e: Exception) {
                throw LoginException("Logout failed: ${e.message}", e)
            }
        }
    }

    /**
     * Vérifie la version de l'application avec le serveur.
     *
     * @param idMbr ID du membre
     * @param deviceId Identifiant unique du device
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun checkVersion(idMbr: String, deviceId: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                loginManager.checkVersion(idMbr, deviceId).get()
            } catch (e: Exception) {
                throw LoginException("Version check failed: ${e.message}", e)
            }
        }
    }

    /**
     * Nettoie les données de connexion.
     */
    fun clearLoginData() {
        loginManager.clearLoginData()
    }

    /**
     * Définit les informations du device.
     *
     * @param version Version de l'application
     * @param phoneName Nom du téléphone
     * @param phoneModel Modèle du téléphone
     * @param phoneBuild Version du build
     */
    fun setDeviceInfo(version: String, phoneName: String, phoneModel: String, phoneBuild: String) {
        loginManager.setDeviceInfo(version, phoneName, phoneModel, phoneBuild)
    }

    /**
     * Définit le callback pour les événements de connexion.
     *
     * @param callback Callback pour les événements de connexion
     */
    fun setLoginCallback(callback: LoginInterfaces.LoginCallback) {
        loginManager.setLoginCallback(callback)
    }

    /**
     * Exception personnalisée pour les erreurs de connexion.
     */
    class LoginException(message: String, cause: Throwable) : Exception(message, cause)

}
