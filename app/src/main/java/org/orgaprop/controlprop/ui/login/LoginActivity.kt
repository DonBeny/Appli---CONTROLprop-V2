package org.orgaprop.controlprop.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View

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
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.LoginViewModel

class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModel()
    private val secureCredentialsManager: SecureCredentialsManager by inject()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d(TAG, "onCreate: Activité créée")
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
                is LoginState.Idle -> showWait(false)
                else -> {}
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
                else -> {}
            }
        })

        viewModel.networkState.observe(this, Observer { isConnected ->
            if (!isConnected) {
                UiUtils.showInfoSnackbar(binding.root, getString(R.string.no_network))
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
                LogUtils.e(TAG, "Erreur lors de la récupération des données utilisateur", e)
                showError("Erreur lors de la déconnexion. Veuillez réessayer.")
            }
        }
    }



    private fun clearLoginData() {
        clearUserData()

        //clearAllData()

        binding.mainActivityUsernameTxt.text.clear()
        binding.mainActivityPasswordTxt.text.clear()

        showWait(false)

        UiUtils.showSuccessSnackbar(binding.root, "Déconnexion réussie")
    }
    private fun checkUserLoggedIn() {
        val userData = getUserData()

        if (userData != null) {
            LogUtils.d(TAG, "checkUserLoggedIn: L'utilisateur est déjà connecté")
            binding.mainActivityConnectLyt.visibility = View.GONE
            binding.mainActivityDecoLyt.visibility = View.VISIBLE
        } else {
            LogUtils.d(TAG, "checkUserLoggedIn: L'utilisateur n'est pas connecté")
            binding.mainActivityConnectLyt.visibility = View.VISIBLE
            binding.mainActivityDecoLyt.visibility = View.GONE

            LogUtils.d(TAG, "checkUserLoggedIn: Load saved credentials")
            loadSavedCredentials()
        }
    }
    private fun loadSavedCredentials() {
        val username = secureCredentialsManager.getUsername()
        val password = secureCredentialsManager.getPassword()
        val adrMac = secureCredentialsManager.getMacAddress()

        LogUtils.d(TAG, "loadSavedCredentials: Username: $username, Password: ****")

        if (username != null && password != null && adrMac != null) {
            binding.mainActivityUsernameTxt.setText(username)
            binding.mainActivityPasswordTxt.setText(password)
            viewModel.checkLogin(username, password, adrMac)
        }
    }



    private fun showWait(show: Boolean) {
        if (show) {
            UiUtils.showProgressDialog(this, getString(R.string.wait))
        } else {
            UiUtils.dismissCurrentDialog()
        }
    }
    private fun showError(message: String) {
        if (message.isNotEmpty()) {
            UiUtils.showErrorSnackbar(binding.root, message)
        }
        showWait(false)
    }

    private fun openWebPage() {
        val url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    private fun launchSelectEntryActivity(data: LoginData) {
        UiUtils.showSuccessSnackbar(binding.root, "Connexion réussie")

        val username = binding.mainActivityUsernameTxt.text.toString()
        val password = binding.mainActivityPasswordTxt.text.toString()

        secureCredentialsManager.saveCredentials(username, password)
        secureCredentialsManager.saveMacAddress(data.adrMac)
        secureCredentialsManager.saveUserId(data.idMbr)

        setUserData(data, username, password)



        showWait(false)

        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

}
