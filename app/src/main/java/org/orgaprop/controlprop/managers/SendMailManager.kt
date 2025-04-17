package org.orgaprop.controlprop.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.io.IOException

import org.json.JSONObject

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class SendMailManager(
    private val httpTask: HttpTask,
    private val context: Context
) {

    companion object {
        private const val TAG = "SendMailManager"

        private const val MAX_IMAGE_WIDTH = 640
        private const val MAX_IMAGE_HEIGHT = 640
        private const val IMAGE_QUALITY = 85
    }



    /**
     * Envoie un email avec les informations fournies.
     *
     * @param idMbr Identifiant du membre
     * @param adrMac Adresse MAC de l'appareil
     * @param typeSend Type d'envoi (tech, ctrl, plan, auto)
     * @param dest1 Adresse email du destinataire principal
     * @param dest2 Adresse email du destinataire 2 (optionnel)
     * @param dest3 Adresse email du destinataire 3 (optionnel)
     * @param dest4 Adresse email du destinataire 4 (optionnel)
     * @param message Contenu du message
     * @param photoUri URI de la photo à joindre (optionnel)
     * @param photoPath Chemin de la photo à joindre (alternative à photoUri, optionnel)
     * @param entry Identifiant de l'entrée concernée (optionnel)
     * @return Résultat de l'opération
     */
    suspend fun sendMail(
        idMbr: Int,
        adrMac: String,
        typeSend: String?,
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String,
        message: String,
        photoUri: Uri? = null,
        photoPath: String? = null,
        entry: String?,
        timer: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (isValid, errorMsg) = validateParams(idMbr, adrMac, dest1, dest2, dest3, dest4)
            if (!isValid) {
                return@withContext Result.failure(
                    BaseException(ErrorCodes.INVALID_INPUT, errorMsg)
                )
            }

            // Préparer les paramètres de base
            val paramsPost = buildBasicParams(idMbr, adrMac, dest1, dest2, dest3, dest4, message, entry, timer)
            val cblParam = getCblParam(typeSend)

            var bitmap: Bitmap? = null

            try {
                bitmap = when {
                    photoUri != null -> loadBitmapFromUri(photoUri)
                    photoPath != null -> BitmapFactory.decodeFile(photoPath)
                    else -> null
                }

                bitmap?.let { originalBitmap ->
                    val optimizedBitmap = resizeBitmapIfNeeded(originalBitmap)
                    val pngBitmap = ensurePngFormat(optimizedBitmap)

                    val base64Image = Base64.encodeToString(
                        bitmapToByteArray(pngBitmap, Bitmap.CompressFormat.PNG),
                        Base64.NO_WRAP
                    )

                    paramsPost.add("image" to base64Image)
                }

                val response = executeHttpRequest(cblParam, paramsPost)

                LogUtils.json(TAG, "sendMail: response received", response)

                return@withContext parseServerResponse(response)
            } finally {
                bitmap?.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendMail", e)
            Result.failure(convertExceptionToBaseException(e))
        }
    }
    private fun bitmapToByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray {
        ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(format, 100, outputStream)
            return outputStream.toByteArray()
        }
    }
    private fun ensurePngFormat(bitmap: Bitmap): Bitmap {
        val outputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        val pngBytes = outputStream.toByteArray()
        val pngBitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)

        outputStream.close()

        return pngBitmap ?: bitmap
    }



    /**
     * Charge un bitmap depuis un URI.
     *
     * @param uri URI de l'image
     * @return Bitmap chargé ou null en cas d'erreur
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading bitmap from URI", e)
            null
        }
    }

    /**
     * Redimensionne un bitmap si nécessaire pour respecter les dimensions maximales.
     *
     * @param bitmap Bitmap à redimensionner
     * @return Bitmap redimensionné ou original si les dimensions sont acceptables
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        return if (bitmap.width > MAX_IMAGE_WIDTH || bitmap.height > MAX_IMAGE_HEIGHT) {
            val width = bitmap.width
            val height = bitmap.height

            val ratio = minOf(
                MAX_IMAGE_WIDTH.toFloat() / width,
                MAX_IMAGE_HEIGHT.toFloat() / height
            )

            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()

            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }



    /**
     * Construit la liste des paramètres de base pour la requête HTTP.
     */
    private fun buildBasicParams(
        idMbr: Int,
        adrMac: String,
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String,
        message: String,
        entry: String?,
        timer: String?
    ): MutableList<Pair<String, String>> {
        val params = mutableListOf(
            "dest1" to dest1,
            "dest2" to dest2,
            "dest3" to dest3,
            "dest4" to dest4,
            "msg" to message,
            "mbr" to idMbr.toString(),
            "mac" to adrMac
        )

        LogUtils.json(TAG, "buildBasicParams: params", params)

        if (!entry.isNullOrBlank()) {
            LogUtils.json(TAG, "buildBasicParams: entry", entry)

            params.add("entry" to entry)
        }
        if (timer != null) {
            LogUtils.json(TAG, "buildBasicParams: timer", timer)

            params.add("timer" to timer)
        }

        return params
    }

    /**
     * Détermine le paramètre CBL en fonction du type d'envoi.
     */
    private fun getCblParam(typeSend: String?): String {
        return when (typeSend) {
            SendMailActivity.SEND_MAIL_ACTIVITY_PROBLEM_TECH -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_TECH
            SendMailActivity.SEND_MAIL_ACTIVITY_CTRL -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_CTRL
            SendMailActivity.SEND_MAIL_ACTIVITY_PLAN -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_PLAN
            SendMailActivity.SEND_MAIL_ACTIVITY_AUTO -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_AUTO
            else -> ""
        }
    }

    /**
     * Exécute la requête HTTP.
     */
    private suspend fun executeHttpRequest(
        cblParam: String,
        paramsPost: List<Pair<String, String>>
    ): String {
        return httpTask.executeHttpTask(
            HttpTaskConstantes.HTTP_TASK_ACT_SEND,
            cblParam,
            "",
            paramsPost.joinToString("&") { "${it.first}=${it.second}" }
        )
    }

    /**
     * Parse la réponse du serveur et renvoie un Result.
     */
    private fun parseServerResponse(response: String): Result<Unit> {
        val jsonObject = JSONObject(response)

        LogUtils.json(TAG, "parseServerResponse: jsonObject", jsonObject)

        return if (jsonObject.getBoolean("status")) {
            Result.success(Unit)
        } else {
            val errorTxt = jsonObject.optJSONObject("error")?.optString("txt")
                ?: "Erreur lors de l'envoi"
            Result.failure(BaseException(ErrorCodes.INVALID_RESPONSE, errorTxt))
        }
    }



    /**
     * Convertit une exception standard en BaseException.
     */
    private fun convertExceptionToBaseException(e: Exception): BaseException {
        return when (e) {
            is BaseException -> e
            is IOException -> BaseException(ErrorCodes.NETWORK_ERROR, "Erreur réseau", e)
            else -> BaseException(ErrorCodes.UNKNOWN_ERROR, e.message, e)
        }
    }



    /**
     * Vérifie la validité des paramètres d'envoi.
     *
     * @param idMbr Identifiant du membre
     * @param adrMac Adresse MAC de l'appareil
     * @param dest1 Adresse email du destinataire principal
     * @param dest2 Adresse email du destinataire 2
     * @param dest3 Adresse email du destinataire 3
     * @param dest4 Adresse email du destinataire 4
     * @return Paire contenant un booléen de validation et un message d'erreur optionnel
     */
    private fun validateParams(
        idMbr: Int,
        adrMac: String,
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String
    ): Pair<Boolean, String?> {
        if (idMbr <= 0) {
            return Pair(false, "Identifiant membre invalide")
        }

        if (adrMac.isBlank()) {
            return Pair(false, "Adresse MAC invalide")
        }

        if (dest1.isBlank() && dest2.isBlank() && dest3.isBlank() && dest4.isBlank()) {
            return Pair(false, "Aucun destinataire spécifié")
        }

        return Pair(true, null)
    }

}
