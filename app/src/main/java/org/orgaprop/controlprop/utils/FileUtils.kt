package org.orgaprop.controlprop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private const val TAG = "FileUtils"
    private const val DEFAULT_QUALITY = 90

    /**
     * Sauvegarde un bitmap dans le répertoire cache de l'application.
     *
     * @param context Contexte de l'application
     * @param bitmap Bitmap à sauvegarder
     * @param filename Nom du fichier
     * @return Fichier créé
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): File {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, out)
        }
        return file
    }

    /**
     * Convertit un bitmap en Uri en le sauvegardant dans le cache.
     *
     * @param context Contexte de l'application
     * @param bitmap Bitmap à convertir
     * @return Uri du fichier créé
     */
    fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val file = saveBitmapToCache(context, bitmap, "temp_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Convertit un bitmap en chaîne Base64.
     *
     * @param bitmap Bitmap à convertir
     * @param format Format de compression (JPEG par défaut)
     * @param quality Qualité de compression (0-100)
     * @return Chaîne encodée en Base64
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = DEFAULT_QUALITY
    ): String {
        ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(format, quality, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }
    }
    /**
     * Convertit une chaîne Base64 en bitmap.
     *
     * @param base64String Chaîne encodée en Base64
     * @return Bitmap décodé ou null en cas d'erreur
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64 to bitmap", e)
            null
        }
    }



    /**
     * Crée un fichier image temporaire.
     *
     * @param context Contexte de l'application
     * @return Fichier créé et son chemin absolu
     */
    @Throws(IOException::class)
    fun createImageFile(context: Context): Pair<File, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val file = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return Pair(file, file.absolutePath)
    }



    /**
     * Charge un bitmap depuis un Uri avec correction de l'orientation.
     *
     * @param context Contexte de l'application
     * @param uri Uri de l'image
     * @param maxWidth Largeur maximale (0 = pas de limite)
     * @param maxHeight Hauteur maximale (0 = pas de limite)
     * @return Bitmap chargé et correctement orienté ou null en cas d'erreur
     */
    fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 0,
        maxHeight: Int = 0
    ): Bitmap? {
        try {
            // Étape 1: Lire les dimensions de l'image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // Étape 2: Calculer le facteur d'échantillonnage si nécessaire
            var sampleSize = 1
            if (maxWidth > 0 && maxHeight > 0) {
                val imageWidth = options.outWidth
                val imageHeight = options.outHeight

                sampleSize = calculateSampleSize(imageWidth, imageHeight, maxWidth, maxHeight)
            }

            // Étape 3: Décoder le bitmap avec le facteur d'échantillonnage
            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }

            // Étape 4: Charger le bitmap et corriger son orientation
            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                bitmap = BitmapFactory.decodeStream(inputStream, null, decodingOptions)
            }

            return bitmap?.let { fixBitmapOrientation(context, it, uri) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI", e)
            return null
        }
    }

    /**
     * Calcule le facteur d'échantillonnage pour le chargement d'une image.
     */
    private fun calculateSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var sampleSize = 1

        if (width > reqWidth || height > reqHeight) {
            val halfWidth = width / 2
            val halfHeight = height / 2

            while ((halfWidth / sampleSize) >= reqWidth &&
                (halfHeight / sampleSize) >= reqHeight) {
                sampleSize *= 2
            }
        }

        return sampleSize
    }
    /**
     * Corrige l'orientation d'un bitmap en fonction des données EXIF.
     */
    fun fixBitmapOrientation(context: Context, bitmap: Bitmap?, uri: Uri): Bitmap? {
        if (bitmap == null) return null

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                return bitmap
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.preScale(1f, -1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.preScale(-1f, 1f)
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.preScale(-1f, 1f)
                    matrix.postRotate(270f)
                }
            }

            return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing bitmap orientation", e)
            return bitmap
        }
    }
    /**
     * Obtient l'orientation EXIF d'une image.
     */
    private fun getExifOrientation(context: Context, uri: Uri, inputStream: InputStream): Int {
        try {
            val exif = ExifInterface(inputStream)
            return exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting EXIF orientation", e)
        }

        return ExifInterface.ORIENTATION_NORMAL
    }



    /**
     * Redimensionne un bitmap pour respecter les dimensions maximales.
     *
     * @param bitmap Bitmap à redimensionner
     * @param maxWidth Largeur maximale
     * @param maxHeight Hauteur maximale
     * @return Bitmap redimensionné ou original si les dimensions sont acceptables
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }



    /**
     * Supprime tous les fichiers temporaires du cache.
     *
     * @param context Contexte de l'application
     * @param prefix Préfixe des fichiers à supprimer (null = tous)
     * @return Nombre de fichiers supprimés
     */
    fun clearCacheFiles(context: Context, prefix: String? = null): Int {
        var count = 0
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (prefix == null || file.name.startsWith(prefix)) {
                    if (file.delete()) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache files", e)
        }
        return count
    }

}
