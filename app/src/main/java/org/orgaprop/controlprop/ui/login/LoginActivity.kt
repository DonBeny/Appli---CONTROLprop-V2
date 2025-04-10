package org.orgaprop.controlprop.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivityLoginBinding
import org.orgaprop.controlprop.security.SecureCredentialsManager
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.getMail.GetMailActivity
import org.orgaprop.controlprop.ui.login.states.LoginState
import org.orgaprop.controlprop.ui.login.states.LogoutState
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.LoginViewModel

class LoginActivity : BaseActivity() {

    private val TAG = "LoginActivity"

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModel()
    private val secureCredentialsManager: SecureCredentialsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activité créée")
    }

    override fun initializeComponents() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setupComponents() {
        setupObservers()
        setupListeners()
        checkUserLoggedIn()
    }

    override fun setupObservers() {
        viewModel.loginState.observe(this, Observer { state ->
            when (state) {
                is LoginState.Loading -> showWait(true)
                is LoginState.Success -> launchSelectEntryActivity(state.data)
                is LoginState.LoggedOut -> clearLoginData()
                is LoginState.Error -> showError(state.message)
            }
        })

        viewModel.logoutState.observe(this, Observer { state ->
            when (state) {
                is LogoutState.Loading -> showWait(true)
                is LogoutState.Success -> {
                    clearLoginData()
                    finishAffinity()
                }
                is LogoutState.Error -> {
                    showError(state.message)
                    clearUserData()
                    finishAffinity()
                }
            }
        })

        // Observer l'état du réseau
        viewModel.networkState.observe(this, Observer { isConnected ->
            if (!isConnected) {
                Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun setupListeners() {
        binding.mainActivityRgpdTxt.setOnClickListener {
            openWebPage()
        }

        binding.mainActivityMailBtn.setOnClickListener {
            val intent = Intent(this, GetMailActivity::class.java)
            startActivity(intent)
        }

        binding.mainActivityConnectBtn.setOnClickListener {
            val check = binding.mainActivityRgpdChx.isChecked
            val username = binding.mainActivityUsernameTxt.text.toString()
            val password = binding.mainActivityPasswordTxt.text.toString()

            if (!check) {
                showError(getString(R.string.error_rgpd))
                return@setOnClickListener
            }

            viewModel.login(username, password)
        }

        binding.mainActivityDecoBtn.setOnClickListener {
            try {
                val userData = getUserData()

                if (userData != null) {
                    viewModel.logout(userData.idMbr, userData.adrMac)
                } else {
                    showError("Aucune donnée utilisateur trouvée. Veuillez vous reconnecter.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des données utilisateur", e)
                showError("Erreur lors de la déconnexion. Veuillez réessayer.")
            }
        }
    }

    private fun launchSelectEntryActivity(data: LoginData) {
        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()

        val username = binding.mainActivityUsernameTxt.text.toString()
        val password = binding.mainActivityPasswordTxt.text.toString()

        // Sauvegarder les identifiants de manière sécurisée
        secureCredentialsManager.saveCredentials(username, password)
        secureCredentialsManager.saveMacAddress(data.adrMac)
        secureCredentialsManager.saveUserId(data.idMbr)

        // Sauvegarder les données utilisateur pour l'application
        setUserData(data, username, password)

        showWait(false)

        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun clearLoginData() {
        // Effacer les données utilisateur dans l'application
        clearUserData()

        // Effacer les identifiants sécurisés
        secureCredentialsManager.clearAllCredentials()

        // Effacer les champs du formulaire
        binding.mainActivityUsernameTxt.text.clear()
        binding.mainActivityPasswordTxt.text.clear()

        showWait(false)

        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedCredentials() {
        // Récupérer les identifiants de manière sécurisée
        val username = secureCredentialsManager.getUsername()
        val password = secureCredentialsManager.getPassword()
        val adrMac = secureCredentialsManager.getMacAddress()

        Log.d(TAG, "loadSavedCredentials: Username: $username, Password: ****")

        if (username != null && password != null && adrMac != null) {
            binding.mainActivityUsernameTxt.setText(username)
            binding.mainActivityPasswordTxt.setText(password)
            viewModel.checkLogin(username, password, adrMac)
        }
    }

    private fun checkUserLoggedIn() {
        val userData = getUserData()

        if (userData != null) {
            Log.d(TAG, "checkUserLoggedIn: L'utilisateur est déjà connecté")
            binding.mainActivityConnectLyt.visibility = View.GONE
            binding.mainActivityDecoLyt.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "checkUserLoggedIn: L'utilisateur n'est pas connecté")
            binding.mainActivityConnectLyt.visibility = View.VISIBLE
            binding.mainActivityDecoLyt.visibility = View.GONE

            Log.d(TAG, "checkUserLoggedIn: Load saved credentials")
            loadSavedCredentials()
        }
    }

    private fun showWait(show: Boolean) {
        binding.mainActivityWaitImg.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        if (message.isNotEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        showWait(false)
    }

    private fun openWebPage() {
        val url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}