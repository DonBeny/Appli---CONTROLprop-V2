package org.orgaprop.controlprop.ui.launch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.managers.PermissionManager
import org.orgaprop.controlprop.viewmodels.LaunchViewModel

class LaunchActivity : AppCompatActivity() {

    private val TAG = "LaunchActivity"

    private val viewModel: LaunchViewModel by viewModels()

    private lateinit var permissionManager: PermissionManager
    //private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Installing SplashScreen")
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Initialize components")
        initializeComponents()

        Log.d(TAG, "onCreate: Installing observers")
        observeViewModel()

        Log.d(TAG, "onCreate: Checking permissions")
        checkPermissionsAndUpdate()
    }

    private fun initializeComponents() {
        //binding = ActivityLaunchBinding.inflate(layoutInflater)
        //setContentView(binding.root)

        permissionManager = PermissionManager(this)

        //updateManager = UpdateManager(this)
        //updateManager.initializeLauncher(::handleUpdateResult)

        //displayVersion()

        Log.d(TAG, "initialized");
    }

    private fun observeViewModel() {
        viewModel.navigationEvent.observe(this) { navigateToMain() }
        viewModel.errorEvent.observe(this) { showError(it) }

        Log.d(TAG, "observed");
    }

    private fun checkPermissionsAndUpdate() {
        permissionManager.checkRequiredPermissions(object : PermissionManager.PermissionResultCallback {
            override fun onResult(granted: Boolean) {
                if (granted) {
                    //startUpdateCheck()

                    Log.d(TAG, "Permission granted")
                    navigateToMain()
                } else {
                    showError(getString(R.string.error_permission_denied))
                }
            }

            override fun onError(e: Exception) {
                showError(e.message ?: "Erreur inconnue")
            }
        })
    }
/*
    private fun startUpdateCheck() {
        if (isFromPlayStore()) {
            updateManager.checkForUpdates(
                onNoUpdate = { viewModel.navigateToMain() },
                onError = { showError(getString(R.string.error_update_check)) }
            )
        } else {
            delayedNavigateToMain()
        }
    }

    private fun delayedNavigateToMain() {
        viewModel.delayedNavigateToMain()
    }
    */
    private fun navigateToMain() {
        Log.d(TAG, "navigateToMain: Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
/*
    private fun handleUpdateResult(success: Boolean) {
        if (success) {
            viewModel.navigateToMain()
        } else {
            updateManager.checkUpdateCriticality { isCritical ->
                if (isCritical) {
                    showCriticalUpdateDialog()
                } else {
                    showUpdateWarning()
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
            .setPositiveButton(R.string.retry) { _, _ -> startUpdateCheck() }
            .setNegativeButton(R.string.quit) { _, _ -> finish() }
            .show()
    }

    private fun showUpdateWarning() {
        Toast.makeText(this, R.string.update_warning_message, Toast.LENGTH_LONG).show()
    }

    private fun isFromPlayStore(): Boolean {
        return try {
            val installer = packageManager.getInstallerPackageName(packageName)
            installer == "com.android.vending"
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.PERMISSION_ERROR, "Impossible de déterminer l'origine de l'installation", e)
        }
    }
*/
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}
