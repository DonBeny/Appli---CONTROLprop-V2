package org.orgaprop.controlprop.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import androidx.lifecycle.Observer

import org.json.JSONException
import org.json.JSONObject

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivityMainBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.HomeActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.main.viewmodels.MainViewModel

class MainActivity : BaseActivity() {

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Activité créée")
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Initialize components")
        initializeComponents()

        Log.d(TAG, "onCreate: Setup components")
        setupComponents()
    }

    // Implémentation des méthodes abstraites de BaseActivity
    override fun initializeComponents() {
        // Initialisation des composants de l'activité
        // Par exemple, initialiser les vues ou les variables
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setupComponents() {
        // Configuration des composants de l'activité
        viewModel.loginState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.LoginState.Loading -> showWait(true)
                is MainViewModel.LoginState.Success -> startAppli(state.data)
                is MainViewModel.LoginState.LoggedOut -> clearLoginData()
                is MainViewModel.LoginState.Error -> showError(state.message)
            }
        })
        viewModel.logoutState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.LogoutState.Loading -> showWait(true)
                is MainViewModel.LogoutState.Success -> finishAffinity()
                is MainViewModel.LogoutState.Error -> {
                    showError(state.message)
                    finishAffinity()
                }
            }
        })

        binding.mainActivityConnectBtn.setOnClickListener {
            val username = binding.mainActivityUsernameTxt.text.toString()
            val password = binding.mainActivityPasswordTxt.text.toString()
            viewModel.login(username, password)
        }
        binding.mainActivityDecoBtn.setOnClickListener {
            val userData = getUserData()

            if (userData != null) {
                viewModel.logout(userData.idMbr, userData.adrMac)
            } else {
                showError("Aucune donnée utilisateur trouvée. Veuillez vous reconnecter.")
            }
        }
    }

    private fun startAppli(data: LoginData) {
        // Logique pour démarrer l'application après une connexion réussie
        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()

        // Enregistrer les données dans BaseActivity
        setUserData(data)

        // naviguer vers une autre activité
        val intent = Intent(this, HomeActivity::class.java) // TODO
        startActivity(intent)
        finish()
    }

    private fun clearLoginData() {
        // Effacer les données de connexion
        clearUserData()

        // Effacer les champs de saisie
        binding.mainActivityUsernameTxt.text.clear()
        binding.mainActivityPasswordTxt.text.clear()
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
    }

    private fun showWait(show: Boolean) {
        // Afficher ou masquer l'indicateur de chargement
        if (show) {
            binding.mainActivityWaitImg.visibility = View.VISIBLE
        } else {
            binding.mainActivityWaitImg.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        // Afficher un message d'erreur
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun enableLoginButton() {
        // Activer le bouton de connexion
        binding.mainActivityConnectBtn.isEnabled = true
    }

    private fun handleVersionSuccess(response: JSONObject) {
        // Logique pour gérer la réponse de vérification de version
        try {
            val version = response.getInt("version")
            Toast.makeText(this, "Version vérifiée: $version", Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            showError("Erreur lors du traitement de la réponse de version")
        }
    }

}
