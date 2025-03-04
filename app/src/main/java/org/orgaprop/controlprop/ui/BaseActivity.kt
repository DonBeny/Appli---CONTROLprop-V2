package org.orgaprop.controlprop.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.orgaprop.controlprop.services.Prefs
import org.orgaprop.controlprop.utils.UiUtils

/**
 * Classe mère pour toutes les activités de l'application.
 * Gère les données partagées, les préférences, et les fonctionnalités communes.
 */
abstract class BaseActivity : AppCompatActivity() {

    // Données partagées
    protected val dataStore = mutableMapOf<String, Any>()
    protected lateinit var preferences: SharedPreferences
    protected lateinit var prefs: Prefs

    // ViewModel
    protected inline fun <reified T : ViewModel> getViewModel(): T {
        return ViewModelProvider(this)[T::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSharedData()
        initializeComponents()
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
     * Affiche un message d'erreur.
     *
     * @param messageId L'ID du message à afficher.
     */
    protected fun showError(@StringRes messageId: Int) {
        UiUtils.showToast(this, messageId)
    }

    /**
     * Affiche ou masque un indicateur de chargement.
     *
     * @param show true pour afficher l'indicateur, false pour le masquer.
     */
    protected fun showWait(show: Boolean) {
        // À implémenter dans les classes filles si nécessaire
    }

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

}
