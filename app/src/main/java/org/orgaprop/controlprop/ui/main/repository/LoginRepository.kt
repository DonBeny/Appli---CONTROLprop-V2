package org.orgaprop.controlprop.ui.main.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.ui.main.interfaces.LoginInterfaces

class LoginRepository(private val loginManager: LoginManager) {

    private var loginCallback: LoginInterfaces.LoginCallback? = null

    /**
     * Tente de connecter un utilisateur.
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @param remember Si true, sauvegarde les credentials
     * @return JSONObject contenant la réponse du serveur
     */
    suspend fun login(username: String, password: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val response = loginManager.login(username, password)
                loginCallback?.onLoginSuccess(response) // Notifier le succès
                response
            } catch (e: BaseException) {
                // Relancer l'exception si elle est déjà une BaseException
                throw e
            } catch (e: Exception) {
                loginCallback?.onLoginFailure(e.message ?: "Login failed") // Notifier l'échec
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
    suspend fun logout(idMbr: Int, adrMac: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val response = loginManager.logout(idMbr, adrMac)
                loginCallback?.onLogoutSuccess() // Notifier le succès
                response
            } catch (e: BaseException) {
                // Relancer l'exception si elle est déjà une BaseException
                throw e
            } catch (e: Exception) {
                loginCallback?.onLogoutFailure(e.message ?: "Logout failed") // Notifier l'échec
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
                val response = loginManager.checkVersion(idMbr, deviceId)
                loginCallback?.onVersionCheckSuccess(response) // Notifier le succès
                response
            } catch (e: BaseException) {
                // Relancer l'exception si elle est déjà une BaseException
                throw e
            } catch (e: Exception) {
                loginCallback?.onVersionCheckFailure(e.message ?: "Version check failed") // Notifier l'échec
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
     * Définit le callback pour les événements de connexion.
     *
     * @param callback Callback pour les événements de connexion
     */
    fun setLoginCallback(callback: LoginInterfaces.LoginCallback) {
        this.loginCallback = callback
    }

    /**
     * Exception personnalisée pour les erreurs de connexion.
     */
    class LoginException(message: String, cause: Throwable) : BaseException(ErrorCodes.LOGIN_FAILED, message, cause)

}
