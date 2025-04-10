package org.orgaprop.controlprop.ui.login.interfaces

import org.json.JSONObject

/**
 * Interface pour gérer les événements de connexion.
 */
interface LoginInterfaces {

    /**
     * Callback pour les événements de connexion.
     */
    interface LoginCallback {
        /**
         * Appelé lorsque la connexion est réussie.
         *
         * @param response La réponse du serveur sous forme de JSONObject.
         */
        fun onLoginSuccess(response: JSONObject)

        /**
         * Appelé lorsque la connexion échoue.
         *
         * @param errorMessage Le message d'erreur.
         */
        fun onLoginFailure(errorMessage: String)

        /**
         * Appelé lorsque la déconnexion est réussie.
         */
        fun onLogoutSuccess()

        /**
         * Appelé lorsque la déconnexion échoue.
         *
         * @param errorMessage Le message d'erreur.
         */
        fun onLogoutFailure(errorMessage: String)

        /**
         * Appelé lorsque la vérification de la version est réussie.
         *
         * @param response La réponse du serveur sous forme de JSONObject.
         */
        fun onVersionCheckSuccess(response: JSONObject)

        /**
         * Appelé lorsque la vérification de la version échoue.
         *
         * @param errorMessage Le message d'erreur.
         */
        fun onVersionCheckFailure(errorMessage: String)

    }

}
