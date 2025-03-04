package org.orgaprop.controlprop.ui.splash

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.orgaprop.controlprop.BuildConfig
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivitySplashScreenBinding
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.PermissionManager
import org.orgaprop.controlprop.managers.UpdateManager

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private val viewModel: SplashViewModel by viewModels()

    private val permissionManager = PermissionManager(this)
    private val updateManager = UpdateManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        observeViewModel()
        checkPermissionsAndUpdate()
    }

    private fun initializeComponents() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateManager.initializeLauncher(::handleUpdateResult)
        displayVersion()
    }

    private fun observeViewModel() {
        viewModel.navigationEvent.observe(this) { navigateToMain() }
        viewModel.errorEvent.observe(this) { showError(it) }
    }

    private fun checkPermissionsAndUpdate() {
        permissionManager.checkRequiredPermissions(object : PermissionManager.PermissionResultCallback {
            override fun onResult(granted: Boolean) {
                if (granted) {
                    startUpdateCheck()
                } else {
                    showError(getString(R.string.error_permission_denied))
                }
            }

            override fun onError(e: Exception) {
                showError(e.message ?: "Erreur inconnue")
            }
        })
    }

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

    private fun displayVersion() {
        binding.splashScreenActivityVersionTxt.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
    }

    private fun delayedNavigateToMain() {
        viewModel.delayedNavigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

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

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}
