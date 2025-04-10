package org.orgaprop.controlprop.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

import org.koin.android.ext.android.inject

import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.sync.SyncUtils
import org.orgaprop.controlprop.ui.login.repository.LoginRepository
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.utils.network.NetworkMonitor

/**
 * Classe mère pour toutes les activités de l'application.
 * Gère les données partagées, les préférences, et les fonctionnalités communes.
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private lateinit var securePreferences: SharedPreferences

    protected val loginRepository: LoginRepository by inject()
    protected val NetworkMonitor: NetworkMonitor by inject()
    protected val syncManager: SyncManager by inject()

    private val gson = Gson()

    protected inline fun <reified T : ViewModel> getViewModel(): T {
        return ViewModelProvider(this)[T::class.java]
    }

    companion object {
        const val TAG = "BaseActivity"

        const val PREF_SAVED_ID_NAME = "ControlProp"
        const val PREF_SAVED_SECURE_ID_NAME = "ControlPropSecure"

        const val PREF_SAVED_USER = "userData"
        const val PREF_SAVED_ENTRY_SELECTED = "entrySelected"
        const val PREF_SAVED_PENDING_CONTROLS = "pendingControls"
        const val PREF_SAVED_PROXI = "proxi"
        const val PREF_SAVED_CONTRACT = "contract"
        const val PREF_SAVED_ENTRY_LIST = "entryList"
        const val PREF_SAVED_TYPE_CTRL = "typeCtrl"
        const val PREF_SAVED_CONFIG_CTRL = "configCtrl"
        const val PREF_SAVED_LIST_AGENTS = "listAgents"
        const val PREF_SAVED_LIST_PRESTATES = "listPrestates"
        const val PREF_SAVED_LIST_RANDOM = "listRandom"

        const val PREF_SAVED_USERNAME = "username"
        const val PREF_SAVED_PASSWORD = "password"
        const val PREF_SAVED_ADR_MAC = "adrMac"

        const val PREF_SAVED_CONFIG_CTRL_VISIT = "visite"
        const val PREF_SAVED_CONFIG_CTRL_METEO = "meteo"
        const val PREF_SAVED_CONFIG_CTRL_PROD = "prod"
        const val PREF_SAVED_CONFIG_CTRL_AFF = "aff"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initializeSharedData()
            initializeComponents()
            setupComponents()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'activité", e)
        }

    }

    /**
     * Initialise les données partagées et les préférences.
     */
    private fun initializeSharedData() {
        preferences = getSharedPreferences(PREF_SAVED_ID_NAME, MODE_PRIVATE)

        try {
            // Préférences sécurisées avec chiffrement pour les données sensibles
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            securePreferences = EncryptedSharedPreferences.create(
                applicationContext,
                PREF_SAVED_SECURE_ID_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            Log.d(TAG, "Préférences sécurisées initialisées")
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erreur lors de l'initialisation des préférences sécurisées, fallback sur préférences standard",
                e
            )
            // Fallback sur les préférences standard si l'encryption échoue
            securePreferences = getSharedPreferences(PREF_SAVED_SECURE_ID_NAME, MODE_PRIVATE)

            // Migration des données sensibles existantes si nécessaire
            try {
                migrateSecureDataIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la migration des données sensibles", e)
            }
        }

        Log.d(TAG, "SharedPreferences initialisées")
    }

    /**
     * Migre les données sensibles des préférences standard vers les préférences sécurisées
     * si elles existent déjà dans les préférences standard.
     */
    private fun migrateSecureDataIfNeeded() {
        val username = preferences.getString(PREF_SAVED_USERNAME, null)
        val password = preferences.getString(PREF_SAVED_PASSWORD, null)

        if (!username.isNullOrEmpty() || !password.isNullOrEmpty()) {
            Log.d(TAG, "Migration des données sensibles vers les préférences sécurisées")

            securePreferences.edit().apply {
                if (!username.isNullOrEmpty()) {
                    putString(PREF_SAVED_USERNAME, username)
                    preferences.edit().remove(PREF_SAVED_USERNAME).apply()
                }

                if (!password.isNullOrEmpty()) {
                    putString(PREF_SAVED_PASSWORD, password)
                    preferences.edit().remove(PREF_SAVED_PASSWORD).apply()
                }

                apply()
            }
        }
    }



    /**
     * Initialise les composants de l'activité.
     */
    protected abstract fun initializeComponents()

    /**
     * Configure les composants de l'activité.
     */
    protected abstract fun setupComponents()

    /**
     * Configure les observateurs pour les données LiveData/Flow.
     */
    protected abstract fun setupObservers()

    /**
     * Configure les écouteurs d'événements (clic, etc.).
     */
    protected abstract fun setupListeners()



    /**
     * Efface toutes les données stockées dans les préférences.
     */
    fun clearAllData() {
        preferences.edit().clear().apply()
        securePreferences.edit().clear().apply()
        Log.d(TAG, "Toutes les données ont été effacées")
    }



    /**
     * Enregistre les données de l'utilisateur après une connexion réussie.
     *
     * @param data Les données de l'utilisateur.
     * @param pseudo Le nom d'utilisateur.
     * @param password Le mot de passe.
     */
    fun setUserData(data: LoginData, pseudo: String, password: String) {
        try {
            // Données standard
            val userDataJson = gson.toJson(data)
            preferences.edit().apply {
                putString(PREF_SAVED_USER, userDataJson)
                putString(PREF_SAVED_ADR_MAC, data.adrMac)
                apply()
            }

            // Données sécurisées
            securePreferences.edit().apply {
                putString(PREF_SAVED_USERNAME, pseudo)
                putString(PREF_SAVED_PASSWORD, password)
                apply()
            }

            Log.d(TAG, "Données utilisateur enregistrées avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement des données utilisateur", e)
        }
    }

    /**
     * Récupère les données de l'utilisateur.
     *
     * @return Les données de l'utilisateur, ou null si elles n'existent pas.
     */
    fun getUserData(): LoginData? {
        val userDataJson = preferences.getString(PREF_SAVED_USER, null)
        if (userDataJson != null) {
            try {
                return gson.fromJson(userDataJson, LoginData::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de userData en LoginData", e)
            }
        }
        return null
    }

    /**
     * Efface les données de l'utilisateur lors de la déconnexion.
     */
    fun clearUserData() {
        try {
            preferences.edit().apply {
                remove(PREF_SAVED_USER)
                remove(PREF_SAVED_ENTRY_SELECTED)
                remove(PREF_SAVED_PROXI)
                remove(PREF_SAVED_CONTRACT)
                remove(PREF_SAVED_ENTRY_LIST)
                remove(PREF_SAVED_TYPE_CTRL)
                remove(PREF_SAVED_CONFIG_CTRL)
                remove(PREF_SAVED_LIST_AGENTS)
                remove(PREF_SAVED_LIST_PRESTATES)
                apply()
            }

            Log.d(TAG, "Données utilisateur effacées avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'effacement des données utilisateur", e)
        }
    }



    /**
     * Enregistre l'entrée sélectionnée.
     *
     * @param entry L'entrée sélectionnée.
     */
    fun setEntrySelected(entry: SelectItem) {
        try {
            preferences.edit().apply {
                putString(PREF_SAVED_ENTRY_SELECTED, gson.toJson(entry))
                apply()
            }
            Log.d(TAG, "Entrée sélectionnée enregistrée: ID ${entry.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement de l'entrée sélectionnée", e)
        }
    }

    /**
     * Récupère l'entrée sélectionnée.
     *
     * @return L'entrée sélectionnée, ou null si elle n'existe pas.
     */
    fun getEntrySelected(): SelectItem? {
        return try {
            val entrySelectedJson = preferences.getString(PREF_SAVED_ENTRY_SELECTED, null)
            entrySelectedJson?.let { json ->
                gson.fromJson(json, SelectItem::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de l'entrée sélectionnée", e)
            null
        }
    }

    /**
     * Ajoute ou met à jour un contrôle en attente.
     *
     * @param entry Le contrôle à ajouter ou mettre à jour.
     */
    fun addPendingControl(entry: SelectItem) {
        try {
            val currentControls = syncManager.getPendingControls().toMutableList()

            // Vérifier si le contrôle existe déjà
            val existingIndex = currentControls.indexOfFirst { it.id == entry.id }
            if (existingIndex >= 0) {
                currentControls[existingIndex] = entry
            } else {
                currentControls.add(entry)
            }

            syncManager.savePendingControls(currentControls)
            Log.d(TAG, "Contrôle en attente ajouté/mis à jour: ID ${entry.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'ajout du contrôle en attente", e)
        }
    }
    /**
     * Récupère tous les contrôles en attente.
     *
     * @return La liste des contrôles en attente.
     */
    fun getPendingControls(): List<SelectItem> {
        return syncManager.getPendingControls()
    }
    /**
     * Supprime un contrôle en attente.
     *
     * @param entryId L'ID du contrôle à supprimer.
     */
    fun clearPendingControl(entryId: Int) {
        try {
            val updatedControls = syncManager.getPendingControls().filter { it.id != entryId }
            syncManager.savePendingControls(updatedControls)
            Log.d(TAG, "Contrôle en attente supprimé: ID $entryId")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression du contrôle en attente", e)
        }
    }
    /**
     * Déclenche une synchronisation des données.
     *
     * @param immediate Si true, la synchronisation est immédiate, sinon elle est planifiée
     */
    fun triggerSync(immediate: Boolean = false) {
        SyncUtils.scheduleSync(this, immediate)
        Log.d(TAG, "Synchronisation déclenchée (immédiate: $immediate)")
    }

    /**
     * Définit l'état du paramètre Proxi.
     *
     * @param proxi L'état du paramètre Proxi.
     */
    fun setProxi(proxi: Boolean) {
        preferences.edit().apply {
            putBoolean(PREF_SAVED_PROXI, proxi)
            apply()
        }
        Log.d(TAG, "Paramètre Proxi défini: $proxi")
    }
    /**
     * Indique si le paramètre Proxi est activé.
     *
     * @return true si Proxi est activé, false sinon.
     */
    fun withProxi(): Boolean {
        return preferences.getBoolean(PREF_SAVED_PROXI, false)
    }

    /**
     * Définit l'état du paramètre Contract.
     *
     * @param contract L'état du paramètre Contract.
     */
    fun setContract(contract: Boolean) {
        preferences.edit().apply {
            putBoolean(PREF_SAVED_CONTRACT, contract)
            apply()
        }
        Log.d(TAG, "Paramètre Contract défini: $contract")
    }
    /**
     * Indique si le paramètre Contract est activé.
     *
     * @return true si Contract est activé, false sinon.
     */
    fun withContract(): Boolean {
        return preferences.getBoolean(PREF_SAVED_CONTRACT, false)
    }

    /**
     * Enregistre la liste des entrées.
     *
     * @param entryList La liste des entrées.
     */
    fun setEntryList(entryList: List<SelectItem>) {
        try {
            preferences.edit().apply {
                putString(PREF_SAVED_ENTRY_LIST, gson.toJson(entryList))
                apply()
            }
            Log.d(TAG, "Liste d'entrées enregistrée: ${entryList.size} éléments")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement de la liste d'entrées", e)
        }
    }

    /**
     * Récupère la liste des entrées.
     *
     * @return La liste des entrées, ou null si elle n'existe pas.
     */
    fun getEntryList(): List<SelectItem> {
        try {
            val entryListJson = preferences.getString(PREF_SAVED_ENTRY_LIST, null)

            if (entryListJson != null) {
                val type = object : TypeToken<List<SelectItem>>() {}.type
                return gson.fromJson(entryListJson, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de la liste d'entrées", e)
        }
        return emptyList()
    }



    /**
     * Définit le type de contrôle.
     *
     * @param typeCtrl Le type de contrôle.
     */
    fun setTypeCtrl(typeCtrl: String) {
        preferences.edit().apply {
            putString(PREF_SAVED_TYPE_CTRL, typeCtrl)
            apply()
        }
        Log.d(TAG, "Type de contrôle défini: $typeCtrl")
    }

    /**
     * Récupère le type de contrôle.
     *
     * @return Le type de contrôle, ou null s'il n'existe pas.
     */
    fun getTypeCtrl(): String? {
        return preferences.getString(PREF_SAVED_TYPE_CTRL, null)
    }



    /**
     * Définit la configuration du contrôle.
     *
     * @param configCtrl La configuration du contrôle.
     */
    fun setConfigCtrl(configCtrl: JSONObject) {
        try {
            preferences.edit().apply {
                putString(PREF_SAVED_CONFIG_CTRL, configCtrl.toString())
                apply()
            }
            Log.d(TAG, "Configuration de contrôle enregistrée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement de la configuration de contrôle", e)
        }
    }

    /**
     * Récupère la configuration du contrôle.
     *
     * @return La configuration du contrôle, ou null si elle n'existe pas.
     */
    fun getConfigCtrl(): JSONObject? {
        try {
            val configCtrlJson = preferences.getString(PREF_SAVED_CONFIG_CTRL, null)

            if (configCtrlJson != null) {
                return JSONObject(configCtrlJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de la configuration de contrôle", e)
        }
        return null
    }



    /**
     * Définit la liste des agents.
     *
     * @param listAgents La liste des agents.
     */
    fun setListAgents(listAgents: JSONObject) {
        try {
            preferences.edit().apply {
                putString(PREF_SAVED_LIST_AGENTS, listAgents.toString())
                apply()
            }
            Log.d(TAG, "Liste des agents enregistrée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement de la liste des agents", e)
        }
    }

    /**
     * Récupère la liste des agents.
     *
     * @return La liste des agents, ou null si elle n'existe pas.
     */
    fun getListAgents(): JSONObject? {
        try {
            val listAgentsJson = preferences.getString(PREF_SAVED_LIST_AGENTS, null)

            if (listAgentsJson != null) {
                return JSONObject(listAgentsJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de la liste des agents", e)
        }
        return null
    }

    /**
     * Définit la liste des prestataires.
     *
     * @param listPrestates La liste des prestataires.
     */
    fun setListPrestates(listPrestates: JSONObject) {
        try {
            preferences.edit().apply {
                putString(PREF_SAVED_LIST_PRESTATES, listPrestates.toString())
                apply()
            }
            Log.d(TAG, "Liste des prestataires enregistrée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement de la liste des prestataires", e)
        }
    }

    /**
     * Récupère la liste des prestataires.
     *
     * @return La liste des prestataires, ou null si elle n'existe pas.
     */
    fun getListPrestates(): JSONObject? {
        try {
            val listPrestatesJson = preferences.getString(PREF_SAVED_LIST_PRESTATES, null)

            if (listPrestatesJson != null) {
                return JSONObject(listPrestatesJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de la liste des prestataires", e)
        }
        return null
    }



    /**
     * Récupère le nom d'utilisateur enregistré.
     *
     * @return Le nom d'utilisateur, ou null s'il n'existe pas.
     */
    fun getUsername(): String? {
        return securePreferences.getString(PREF_SAVED_USERNAME, null)
            ?: preferences.getString(PREF_SAVED_USERNAME, null) // Fallback pour la rétrocompatibilité
    }

    /**
     * Récupère le mot de passe enregistré.
     *
     * @return Le mot de passe, ou null s'il n'existe pas.
     */
    fun getPassword(): String? {
        return securePreferences.getString(PREF_SAVED_PASSWORD, null)
            ?: preferences.getString(PREF_SAVED_PASSWORD, null) // Fallback pour la rétrocompatibilité
    }

    /**
     * Récupère l'adresse MAC enregistrée.
     *
     * @return L'adresse MAC, ou null si elle n'existe pas.
     */
    fun getAdrMac(): String? {
        return preferences.getString(PREF_SAVED_ADR_MAC, null)
    }

}
