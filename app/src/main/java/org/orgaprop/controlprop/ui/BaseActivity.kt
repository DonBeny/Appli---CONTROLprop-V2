package org.orgaprop.controlprop.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject

import org.koin.android.ext.android.inject
import org.orgaprop.controlprop.models.SelectItem

import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.utils.network.NetworkMonitor

/**
 * Classe mère pour toutes les activités de l'application.
 * Gère les données partagées, les préférences, et les fonctionnalités communes.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val TAG = "BaseActivity"

    private lateinit var preferences: SharedPreferences
    //protected lateinit var prefs: Prefs

    protected val loginRepository: LoginRepository by inject()
    protected val NetworkMonitor: NetworkMonitor by inject()

    protected inline fun <reified T : ViewModel> getViewModel(): T {
        return ViewModelProvider(this)[T::class.java]
    }



    companion object {
        const val PREF_SAVED_USER = "userData"
        const val PREF_SAVED_ENTRY_SELECTED = "entrySelected"
        const val PREF_SAVED_PROXI = "proxi"
        const val PREF_SAVED_CONTRACT = "contract"
        const val PREF_SAVED_ENTRY_LIST = "entryList"
        const val PREF_SAVED_TYPE_CTRL = "typeCtrl"
        const val PREF_SAVED_CONFIG_CTRL = "configCtrl"
        const val PREF_SAVED_LIST_AGENTS = "listAgents"
        const val PREF_SAVED_LIST_PRESTATES = "listPrestates"

        const val PREF_SAVED_USERNAME = "username"
        const val PREF_SAVED_PASSWORD = "password"
        const val PREF_SAVED_ADR_MAC = "adrMac"

        const val PREF_SAVED_CONFIG_CTRL_VISIT = "visite"
        const val PREF_SAVED_CONFIG_CTRL_METEO = "meteo"
        const val PREF_SAVED_CONFIG_CTRL_PROD = "prod"
        const val PREF_SAVED_CONFIG_CTRL_AFF = "aff"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Activité créée")
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Initialize shared data")
        initializeSharedData()

        Log.d(TAG, "onCreate: Initialize components")
        initializeComponents()

        Log.d(TAG, "onCreate: Setup components")
        setupComponents()
    }

    /**
     * Initialise les données partagées et les préférences.
     */
    private fun initializeSharedData() {
        preferences = getSharedPreferences("ControlProp", MODE_PRIVATE)
        //prefs = Prefs(this)
    }

    /**
     * Initialise les composants de l'activité.
     */
    protected abstract fun initializeComponents()

    /**
     * Configure les composants de l'activité.
     */
    protected abstract fun setupComponents()

    protected abstract fun setupObservers()

    protected abstract fun setupListeners()



    /**
     * Enregistre les données de l'utilisateur après une connexion réussie.
     *
     * @param data Les données de l'utilisateur.
     */
    fun setUserData(data: LoginData, pseudo: String, password: String) {
        Log.d(TAG, "setUserData: $data")

        val gson = Gson()
        val userDataJson = gson.toJson(data)

        Log.d(TAG, "setUserData: userDataJson: $userDataJson")

        //prefs.setMbr(data.idMbr.toString())
        //prefs.setAdrMac(data.adrMac)

        preferences.edit().apply {
            putString(PREF_SAVED_USER, userDataJson)
            putString(PREF_SAVED_USERNAME, pseudo)
            putString(PREF_SAVED_PASSWORD, password)
            putString(PREF_SAVED_ADR_MAC, data.adrMac)
            apply()
        }

        Log.d(TAG, "setUserData: Données enregistrées => "+preferences.getString("username", "null"))
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
                val gson = Gson()
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
    }



    fun setEntrySelected(entry: SelectItem) {
        Log.d(TAG, "setEntrySelected: $entry")

        preferences.edit().apply {
            putString(PREF_SAVED_ENTRY_SELECTED, Gson().toJson(entry))
            apply()
        }

        Log.d(TAG, "setEntrySelected: Données enregistrées => "+preferences.getString(PREF_SAVED_ENTRY_SELECTED, "null"))
    }
    fun getEntrySelected(): SelectItem? {
        val entrySelectedJson = preferences.getString(PREF_SAVED_ENTRY_SELECTED, null)

        if (entrySelectedJson != null) {
            try {
                return Gson().fromJson(entrySelectedJson, SelectItem::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de entrySelected en SelectItem", e)
            }
        }
        return null
    }

    fun setProxi(proxi: Boolean) {
        preferences.edit().apply {
            putBoolean(PREF_SAVED_PROXI, proxi)
            apply()
        }
    }
    fun withProxi(): Boolean {
        return preferences.getBoolean(PREF_SAVED_PROXI, false)
    }

    fun setContract(contract: Boolean) {
        preferences.edit().apply {
            putBoolean(PREF_SAVED_CONTRACT, contract)
            apply()
        }
    }
    fun withContract(): Boolean {
        return preferences.getBoolean(PREF_SAVED_CONTRACT, false)
    }

    fun setEntryList(entryList: List<SelectItem>) {
        Log.d(TAG, "setEntryList: $entryList")

        val gson = Gson()
        val entryListJson = gson.toJson(entryList)

        preferences.edit().apply {
            putString(PREF_SAVED_ENTRY_LIST, entryListJson)
            apply()
        }

        Log.d(TAG, "setEntryList: Données enregistrées => ${preferences.getString(PREF_SAVED_ENTRY_LIST, "null")}")
    }
    fun getEntryList(): List<SelectItem>? {
        val entryListJson = preferences.getString(PREF_SAVED_ENTRY_LIST, null)

        if (entryListJson != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<SelectItem>>() {}.type
                return gson.fromJson(entryListJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de entryList en List<SelectItem>", e)
            }
        }
        return null
    }



    fun setTypeCtrl(typeCtrl: String) {
        preferences.edit().apply {
            putString(PREF_SAVED_TYPE_CTRL, typeCtrl)
            apply()
        }
    }
    fun getTypeCtrl(): String? {
        return preferences.getString(PREF_SAVED_TYPE_CTRL, null)
    }



    fun setConfigCtrl(configCtrl: JSONObject) {
        preferences.edit().apply {
            putString(PREF_SAVED_CONFIG_CTRL, configCtrl.toString())
            apply()
        }
    }
    fun getConfigCtrl(): JSONObject? {
        val configCtrlJson = preferences.getString(PREF_SAVED_CONFIG_CTRL, null)

        if (configCtrlJson != null) {
            try {
                return JSONObject(configCtrlJson)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de configCtrl en JSONObject", e)
                return null
            }
        }
        return null
    }



    fun setListAgents(listAgents: JSONArray) {
        preferences.edit().apply {
            putString(PREF_SAVED_LIST_AGENTS, listAgents.toString())
            apply()
        }
    }
    fun getListAgents(): JSONArray? {
        val listAgentsJson = preferences.getString(PREF_SAVED_LIST_AGENTS, null)

        if (listAgentsJson != null) {
            try {
                return JSONArray(listAgentsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de listAgents en JSONArray", e)
                return null
            }
        }
        return null
    }

    fun setListPrestates(listPrestates: JSONArray) {
        preferences.edit().apply {
            putString(PREF_SAVED_LIST_PRESTATES, listPrestates.toString())
            apply()
        }
    }
    fun getListPrestates(): JSONArray? {
        val listPrestatesJson = preferences.getString(PREF_SAVED_LIST_PRESTATES, null)

        if (listPrestatesJson != null) {
            try {
                return JSONArray(listPrestatesJson)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la conversion de listPrestataires en JSONArray", e)
                return null
            }
        }
        return null
    }



    /**
     * Récupère le nom d'utilisateur enregistré.
     *
     * @return Le nom d'utilisateur, ou null s'il n'existe pas.
     */
    fun getUsername(): String? {
        return preferences.getString(PREF_SAVED_USERNAME, null)
    }

    /**
     * Récupère le mot de passe enregistré.
     *
     * @return Le mot de passe déchiffré, ou null s'il n'existe pas.
     */
    fun getPassword(): String? {
        return preferences.getString(PREF_SAVED_PASSWORD, null)
    }

    /**
     * Récupère l'adresse MAC enregistrée.
     *
     * @return L'adresse MAC, ou null s'il n'existe pas.
     */
    fun getAdrMac(): String? {
        return preferences.getString(PREF_SAVED_ADR_MAC, null)
    }

}
