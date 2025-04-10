package org.orgaprop.controlprop.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentialsManager(private val context: Context) {

    companion object {
        private const val TAG = "SecureCredentialsManager"

        // Noms des préférences sécurisées
        private const val ENCRYPTED_PREF_FILE = "secure_user_credentials"

        // Clés pour les préférences
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_MAC_ADDRESS = "mac_address"
        private const val KEY_USER_ID = "user_id"

        // Paramètres pour Android Keystore
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "OrgaPropCredentialsKey"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREF_FILE,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences", e)
            context.getSharedPreferences(ENCRYPTED_PREF_FILE, Context.MODE_PRIVATE)
        }
    }



    fun saveCredentials(username: String, password: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .apply()

            Log.d(TAG, "Identifiants sauvegardés de manière sécurisée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des identifiants", e)
        }
    }

    /**
     * Récupère le nom d'utilisateur stocké
     * @return Le nom d'utilisateur ou null s'il n'existe pas
     */
    fun getUsername(): String? {
        return encryptedPrefs.getString(KEY_USERNAME, null)
    }

    /**
     * Récupère le mot de passe stocké
     * @return Le mot de passe ou null s'il n'existe pas
     */
    fun getPassword(): String? {
        return encryptedPrefs.getString(KEY_PASSWORD, null)
    }

    fun saveMacAddress(macAddress: String) {
        encryptedPrefs.edit()
            .putString(KEY_MAC_ADDRESS, macAddress)
            .apply()
    }

    /**
     * Récupère l'adresse MAC stockée
     * @return L'adresse MAC ou null si elle n'existe pas
     */
    fun getMacAddress(): String? {
        return encryptedPrefs.getString(KEY_MAC_ADDRESS, null)
    }

    fun saveUserId(userId: Int) {
        encryptedPrefs.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Récupère l'ID utilisateur stocké
     * @return L'ID utilisateur ou -1 s'il n'existe pas
     */
    fun getUserId(): Int {
        return encryptedPrefs.getInt(KEY_USER_ID, -1)
    }

    fun clearAllCredentials() {
        encryptedPrefs.edit().clear().apply()
        Log.d(TAG, "Toutes les données d'identification ont été effacées")
    }

    /**
     * Vérifie si l'utilisateur a des identifiants sauvegardés
     * @return true si des identifiants existent, false sinon
     */
    fun hasCredentials(): Boolean {
        return getUsername() != null && getPassword() != null
    }

}
