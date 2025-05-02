package org.orgaprop.controlprop.exceptions

import org.orgaprop.controlprop.utils.LogUtils

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

    companion object {

        const val TAG = "BaseException"

    }



    // Constructeur 1 : Uniquement le code d'erreur
    constructor(code: Int) : this(code, null, null) {
        LogUtils.e(TAG, "Erreur : ${ErrorCodes.getMessageForCode(code)}")
    }

    // Constructeur 2 : Code d'erreur et message
    constructor(code: Int, message: String?) : this(code, message, null) {
        LogUtils.e(TAG, "Erreur : ${ErrorCodes.getMessageForCode(code)} : ${message ?: ErrorCodes.getMessageForCode(code)}")
    }

    // Constructeur 3 : Code d'erreur et cause (Exception)
    constructor(code: Int, cause: Throwable?) : this(code, null, cause) {
        LogUtils.e(TAG, "Erreur : ${ErrorCodes.getMessageForCode(code)}", cause ?: Exception())
    }

    // Constructeur 4 : Code d'erreur, message et cause (Exception)
    constructor(code: Int, message: String?, cause: Throwable?) : super(message ?: ErrorCodes.getMessageForCode(code), cause) {
        this.code = code
        LogUtils.e(TAG, "Erreur : ${ErrorCodes.getMessageForCode(code)} : ${message ?: ErrorCodes.getMessageForCode(code)}", cause ?: Exception())
    }

}
