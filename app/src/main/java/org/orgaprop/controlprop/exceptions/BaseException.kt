package org.orgaprop.controlprop.exceptions

import android.util.Log

/**
 * Exception de base pour l'application.
 * Permet de gérer les erreurs de manière centralisée.
 *
 * @param message Le message d'erreur (optionnel).
 * @param code Le code d'erreur (optionnel).
 * @param cause La cause de l'exception (optionnelle).
 */
open class BaseException : Exception {

    val code: Int

    // Constructeur 1 : Uniquement le code d'erreur
    constructor(code: Int) : this(code, null, null) {
        Log.e(TAG, "Erreur : ${getMessageForCode(code)}")
    }

    // Constructeur 2 : Code d'erreur et message
    constructor(code: Int, message: String?) : this(code, message, null) {
        Log.e(TAG, "Erreur : ${message ?: getMessageForCode(code)}")
    }

    // Constructeur 3 : Code d'erreur et cause (Exception)
    constructor(code: Int, cause: Exception?) : this(code, null, cause) {
        Log.e(TAG, "Erreur : ${getMessageForCode(code)}", cause)
    }

    // Constructeur 4 : Code d'erreur, message et cause (Exception)
    constructor(code: Int, message: String?, cause: Exception?) : super(message ?: getMessageForCode(code), cause) {
        this.code = code
        Log.e(TAG, "Erreur : ${message ?: getMessageForCode(code)}", cause)
    }

    companion object {

        const val DEFAULT_ERROR_CODE = -1 // Code d'erreur par défaut
        const val TAG = "BaseException"   // Tag pour les logs

        /**
         * Retourne un message par défaut en fonction du code d'erreur.
         *
         * @param code Le code d'erreur.
         * @return Le message correspondant au code d'erreur.
         */
        fun getMessageForCode(code: Int): String {
            return when (code) {
                ErrorCodes.PERMISSION_DENIED -> "Permission refusée"
                ErrorCodes.NETWORK_ERROR -> "Erreur réseau"
                ErrorCodes.DATABASE_ERROR -> "Erreur de base de données"
                else -> "Une erreur s'est produite"
            }
        }

    }

}
