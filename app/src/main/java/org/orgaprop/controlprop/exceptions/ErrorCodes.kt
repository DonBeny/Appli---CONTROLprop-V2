package org.orgaprop.controlprop.exceptions

/**
 * Contient les codes d'erreur de l'application.
 */
object ErrorCodes {

    const val UNKNOWN_ERROR = 1000
    const val NETWORK_ERROR = 1001
    const val INVALID_RESPONSE = 1002

    const val LOGIN_FAILED = 2001
    const val LOGOUT_FAILED = 2002
    const val SESSION_EXPIRED = 2003
    const val UNAUTHORIZED = 2004
    const val PERMISSION_ERROR = 2005

    const val VERSION_CHECK_FAILED = 3001

    const val DATA_NOT_FOUND = 4001
    const val INVALID_DATA = 4002
    const val INVALID_INPUT = 4003

    const val SYNC_FAILED = 5001

    const val TYPE_CTRL_ERROR = 6001
    const val CONFIG_CTRL_ERROR = 6002
    const val PLAN_ACTION_NOT_AVAILABLE = 6003



    /**
     * Retourne un message par défaut en fonction du code d'erreur.
     *
     * @param code Le code d'erreur.
     * @return Le message correspondant au code d'erreur.
     */
    fun getMessageForCode(code: Int): String {
        return when (code) {
            NETWORK_ERROR -> "Erreur réseau lors de la connexion"
            INVALID_RESPONSE -> "Réponse serveur invalide"
            LOGIN_FAILED -> "Échec de la connexion"
            LOGOUT_FAILED -> "Échec de la déconnexion"
            VERSION_CHECK_FAILED -> "Échec de la vérification de version"
            SESSION_EXPIRED -> "Votre session a expiré, veuillez vous reconnecter"
            UNAUTHORIZED -> "Vous n'êtes pas autorisé à effectuer cette action"
            DATA_NOT_FOUND -> "Données non trouvées"
            INVALID_DATA -> "Données invalides"
            SYNC_FAILED -> "Échec de la synchronisation"
            else -> "Une erreur inconnue s'est produite"
        }
    }

}
