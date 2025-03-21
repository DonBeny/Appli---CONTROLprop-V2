package org.orgaprop.controlprop.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import androidx.lifecycle.Observer

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivityMainBinding

import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.getmail.GetMailActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectentry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.MainViewModel


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

        Log.d(TAG, "onCreate: Check user logged in")
        checkUserLoggedIn()
    }



    override fun initializeComponents() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        viewModel.loginState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.LoginState.Loading -> showWait(true)
                is MainViewModel.LoginState.Success -> launchSelectEntryActivity(state.data)
                is MainViewModel.LoginState.LoggedOut -> clearLoginData()
                is MainViewModel.LoginState.Error -> showError(state.message)
            }
        })
        viewModel.logoutState.observe(this, Observer { state ->
            when (state) {
                is MainViewModel.LogoutState.Loading -> showWait(true)
                is MainViewModel.LogoutState.Success -> {
                    clearLoginData()
                    finishAffinity()
                }
                is MainViewModel.LogoutState.Error -> {
                    showError(state.message)
                    clearUserData()
                    finishAffinity()
                }
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
            val userData = getUserData()

            if (userData != null) {
                viewModel.logout(userData.idMbr, userData.adrMac)
            } else {
                showError("Aucune donnée utilisateur trouvée. Veuillez vous reconnecter.")
            }
        }
    }



    private fun launchSelectEntryActivity(data: LoginData) {
        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()

        val username = binding.mainActivityUsernameTxt.text.toString()
        val password = binding.mainActivityPasswordTxt.text.toString()

        setUserData(data, username, password)
        showWait(false)

        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }



    private fun clearLoginData() {
        clearUserData()

        binding.mainActivityUsernameTxt.text.clear()
        binding.mainActivityPasswordTxt.text.clear()

        showWait(false)

        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedCredentials() {
        val username = getUsername()
        val password = getPassword()
        val adrMac = getAdrMac()

        Log.d(TAG, "loadSavedCredentials: Username: $username, Password: $password")

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
        if (show) {
            binding.mainActivityWaitImg.visibility = View.VISIBLE
        } else {
            binding.mainActivityWaitImg.visibility = View.GONE
        }
    }
    private fun showError(message: String) {
        if( message != "" )
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        showWait(false)
    }


    private fun openWebPage() {
        val url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

}
