package org.orgaprop.controlprop.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import org.koin.android.ext.android.inject

import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import org.orgaprop.controlprop.utils.prefs.Prefs

/**
 * Classe mère pour toutes les activités de l'application.
 * Gère les données partagées, les préférences, et les fonctionnalités communes.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val TAG = "BaseActivity"

    // Données partagées
    protected val dataStore = mutableMapOf<String, Any>()
    protected lateinit var preferences: SharedPreferences
    protected lateinit var prefs: Prefs

    // Accès à l'instance de LoginRepository via l'application
    protected val loginRepository: LoginRepository by inject()
    protected val NetworkMonitor: NetworkMonitor by inject()

    // ViewModel
    protected inline fun <reified T : ViewModel> getViewModel(): T {
        return ViewModelProvider(this)[T::class.java]
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
        prefs = Prefs(this)
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
     * Stocke une donnée dans le dataStore.
     *
     * @param key   La clé de la donnée.
     * @param value La valeur de la donnée.
     */
    protected fun putData(key: String, value: Any) {
        if (key.isNotEmpty()) {
            dataStore[key] = value
        }
    }

    /**
     * Récupère une donnée du dataStore.
     *
     * @param key La clé de la donnée.
     * @return La valeur de la donnée, ou null si la clé n'existe pas.
     */
    protected fun getData(key: String): Any? {
        return dataStore[key]
    }

    /**
     * Supprime une donnée partagée.
     *
     * @param key La clé de la donnée.
     */
    fun removeData(key: String) {
        if (key.isNotEmpty()) {
            dataStore.remove(key)
        }
    }
    /**
     * Enregistre les données de l'utilisateur après une connexion réussie.
     *
     * @param data Les données de l'utilisateur.
     */
    fun setUserData(data: LoginData, pseudo: String, password: String) {
        Log.d(TAG, "setUserData: $data")

        putData("userData", data)

        prefs.setMbr(data.idMbr.toString())
        prefs.setAdrMac(data.adrMac)

        preferences.edit().apply {
            putString("username", pseudo)
            putString("password", password)
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
        return getData("userData") as? LoginData
    }

    /**
     * Efface les données de l'utilisateur lors de la déconnexion.
     */
    fun clearUserData() {
        //removeData("userData")
    }

    /**
     * Récupère le nom d'utilisateur enregistré.
     *
     * @return Le nom d'utilisateur, ou null s'il n'existe pas.
     */
    fun getUsername(): String? {
        return preferences.getString("username", null)
    }

    /**
     * Récupère le mot de passe enregistré.
     *
     * @return Le mot de passe déchiffré, ou null s'il n'existe pas.
     */
    fun getPassword(): String? {
        return preferences.getString("password", null)
    }

}
