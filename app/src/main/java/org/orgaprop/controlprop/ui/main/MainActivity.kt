package org.orgaprop.controlprop.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.managers.PermissionManager
import org.orgaprop.controlprop.managers.UpdateManager
import org.orgaprop.controlprop.ui.main.inteface.MainEvent
import org.orgaprop.controlprop.viewmodels.MainViewModel

@SuppressLint("CustomSplashScreen")
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var permissionManager: PermissionManager
    private lateinit var updateManager: UpdateManager

    private var isInitializationComplete = false

    companion object {
        const val TAG = "LaunchActivity"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isInitializationComplete }

        super.onCreate(savedInstanceState)

        initializeComponents()
        observeViewModel()
        checkPermissionsAndUpdate()
    }

    private fun initializeComponents() {
        try {
            permissionManager = PermissionManager(this)
            updateManager = UpdateManager(this)
            updateManager.initializeLauncher(::handleUpdateResult)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            showError("Failed to initialize components")
            completeInitialization()
            viewModel.navigateToMain()
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainEvent.NavigateToMain -> navigateToMain()
                    is MainEvent.ShowError -> showError(event.message)
                }
            }
        }

        Log.d(TAG, "observed")
    }



    private fun checkPermissionsAndUpdate() {
        permissionManager.checkRequiredPermissions(object : PermissionManager.PermissionResultCallback {
            override fun onResult(granted: Boolean) {
                if (granted) {
                    Log.d(TAG, "Permission granted")
                    startUpdateCheck()
                } else {
                    showError(getString(R.string.error_permission_denied))
                    completeInitialization()
                }
            }

            override fun onError(e: Exception) {
                showError(e.message ?: "Erreur inconnue")
                completeInitialization()
            }
        })
    }



    private fun startUpdateCheck() {
        try {
            if (isFromPlayStore()) {
                updateManager.checkForUpdates(
                    onNoUpdate = {
                        completeInitialization()
                        viewModel.navigateToMain()
                    },
                    onError = {
                        showError(getString(R.string.error_update_check))
                        completeInitialization()
                        viewModel.navigateToMain()
                    }
                )
            } else {
                completeInitialization()
                viewModel.navigateToMain()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during update check", e)
            showError(getString(R.string.error_update_check))
            completeInitialization()
            viewModel.navigateToMain()
        }
    }
    private fun handleUpdateResult(success: Boolean) {
        if (success) {
            completeInitialization()
            viewModel.navigateToMain()
        } else {
            updateManager.checkUpdateCriticality { isCritical ->
                if (isCritical) {
                    completeInitialization()
                    showCriticalUpdateDialog()
                } else {
                    showUpdateWarning()
                    completeInitialization()
                    viewModel.navigateToMain()
                }
            }
        }
    }
    private fun showCriticalUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.update_critical_title)
            .setMessage(R.string.update_critical_message)
            .setCancelable(false)
            .setPositiveButton(R.string.retry) { _, _ ->
                startUpdateCheck()
            }
            .setNegativeButton(R.string.quit) { _, _ ->
                finish()
            }
            .show()
    }
    private fun showUpdateWarning() {
        Toast.makeText(this, R.string.update_warning_message, Toast.LENGTH_LONG).show()
    }
    private fun isFromPlayStore(): Boolean {
        return try {
            val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
            installSourceInfo.installingPackageName == "com.android.vending"
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification de la source d'installation", e)
            false
        }
    }



    private fun completeInitialization() {
        isInitializationComplete = true
    }
    private fun navigateToMain() {
        try {
            Log.d(TAG, "navigateToMain: Navigating to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to LoginActivity", e)
            showError("Failed to navigate to LoginActivity")
            finish()
        }
    }



    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}
