package org.orgaprop.controlprop.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.ControlPropApplication
import org.orgaprop.controlprop.databinding.ActivityMainBinding
import org.orgaprop.controlprop.ui.main.viewmodels.MainViewModel
import org.orgaprop.controlprop.ui.main.viewmodels.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as ControlPropApplication).loginRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observer les changements d'état de connexion
        viewModel.loginState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.LoginState.Loading -> showWait(true)
                is MainViewModel.LoginState.Success -> startAppli(state.response)
                is MainViewModel.LoginState.LoggedOut -> clearLoginData()
                is MainViewModel.LoginState.Error -> showError(state.message)
            }
        })

        // Observer les changements d'état de validation des entrées
        viewModel.validationState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.ValidationState.Valid -> enableLoginButton()
                is MainViewModel.ValidationState.Invalid -> showError(state.message)
            }
        })

        // Observer les changements d'état de version
        viewModel.versionState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.VersionState.Loading -> showWait(true)
                is MainViewModel.VersionState.Success -> handleVersionSuccess(state.response)
                is MainViewModel.VersionState.Error -> showError(state.message)
            }
        })

        // Gestion des clics sur les boutons
        binding.mainActivityConnectBtn.setOnClickListener {
            val username = binding.mainActivityUsernameTxt.text.toString()
            val password = binding.mainActivityPasswordTxt.text.toString()
            viewModel.login(username, password, true)
        }

        binding.mainActivityDecoBtn.setOnClickListener {
            val username = binding.mainActivityUsernameTxt.text.toString()
            val password = binding.mainActivityPasswordTxt.text.toString()
            viewModel.logout(username, password)
        }
    }

    private fun startAppli(response: JSONObject) {
        // Logique pour démarrer l'application après une connexion réussie
        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
        // Vous pouvez naviguer vers une autre activité ici
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun clearLoginData() {
        // Logique pour effacer les données de connexion
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
