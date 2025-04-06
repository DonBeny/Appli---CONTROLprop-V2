package org.orgaprop.controlprop.managers

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.function.Consumer

/**
 * Gestionnaire des mises à jour de l'application via le Play Store.
 */
class UpdateManager(private val activity: AppCompatActivity) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATE_REQUEST_CODE = 1000
        private const val UPDATE_STALENESS_DAYS = 3 // Nombre de jours avant de forcer la mise à jour
        private const val UPDATE_PRIORITY_THRESHOLD = 3 // Seuil de priorité pour les mises à jour
    }

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var isUpdateInProgress = false
    private var updateCompleteCallback: Consumer<Boolean>? = null

    /**
     * Initialise le launcher pour gérer le résultat de la demande de mise à jour.
     *
     * @param resultCallback Callback appelé après la tentative de mise à jour.
     */
    fun initializeLauncher(resultCallback: Consumer<Boolean>) {
        updateLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            isUpdateInProgress = false
            resultCallback.accept(result.resultCode == Activity.RESULT_OK)
        }
    }

    /**
     * Vérifie si une mise à jour est disponible et nécessaire.
     *
     * @param onNoUpdate Appelé si aucune mise à jour n'est nécessaire.
     * @param onError    Appelé en cas d'erreur.
     */
    fun checkForUpdates(onNoUpdate: Runnable, onError: Consumer<Exception>) {
        if (isUpdateInProgress) {
            Log.d(TAG, "Une mise à jour est déjà en cours")
            return
        }

        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    handleUpdateInfo(appUpdateInfo, onNoUpdate, onError)
                }
                .addOnFailureListener { e ->
                    handleError(e, onError)
                }
        } catch (e: Exception) {
            handleError(e, onError)
        }
    }

    private fun handleUpdateInfo(
        updateInfo: AppUpdateInfo,
        onNoUpdate: Runnable,
        onError: Consumer<Exception>
    ) {
        if (isUpdateRequired(updateInfo)) {
            isUpdateInProgress = true
            startUpdate(updateInfo, onError)
        } else {
            onNoUpdate.run()
        }
    }

    private fun isUpdateRequired(updateInfo: AppUpdateInfo): Boolean {
        return updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                (updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ||
                        updateInfo.clientVersionStalenessDays() != null &&
                        updateInfo.clientVersionStalenessDays()!! >= UPDATE_STALENESS_DAYS ||
                        updateInfo.updatePriority() >= UPDATE_PRIORITY_THRESHOLD)
    }

    private fun handleError(e: Exception, onError: Consumer<Exception>?) {
        Log.e(TAG, "Erreur lors de la gestion des mises à jour", e)
        isUpdateInProgress = false
        onError?.accept(e)
    }

    private fun startUpdate(updateInfo: AppUpdateInfo, onError: Consumer<Exception>?) {
        if (!::updateLauncher.isInitialized) {
            Log.e(TAG, "UpdateLauncher non initialisé")
            return
        }

        try {
            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
        } catch (e: Exception) {
            handleError(e, onError)
        }
    }

    /**
     * Définit un callback à appeler une fois la mise à jour terminée.
     *
     * @param callback Le callback à appeler.
     */
    fun setUpdateCompleteCallback(callback: Consumer<Boolean>) {
        this.updateCompleteCallback = callback
    }

    /**
     * Vérifie et reprend une mise à jour en cours si nécessaire.
     */
    fun resumeUpdate() {
        if (isUpdateInProgress) {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener(this::handleResumeUpdate)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur lors de la reprise de la mise à jour", e)
                }
        }
    }

    private fun handleResumeUpdate(appUpdateInfo: AppUpdateInfo?) {
        if (appUpdateInfo == null) {
            isUpdateInProgress = false
            return
        }

        when (appUpdateInfo.installStatus()) {
            InstallStatus.DOWNLOADED -> appUpdateManager.completeUpdate()
            InstallStatus.INSTALLED -> {
                isUpdateInProgress = false
                updateCompleteCallback?.accept(true)
            }
            else -> {
                isUpdateInProgress = false
                updateCompleteCallback?.accept(false)
            }
        }
    }

    /**
     * Vérifie la criticité de la mise à jour.
     *
     * @param callback Callback appelé avec le résultat (true si la mise à jour est critique).
     */
    fun checkUpdateCriticality(callback: Consumer<Boolean>) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val isCritical = appUpdateInfo.updatePriority() >= UPDATE_PRIORITY_THRESHOLD ||
                        (appUpdateInfo.clientVersionStalenessDays() != null &&
                                appUpdateInfo.clientVersionStalenessDays()!! >= UPDATE_STALENESS_DAYS)
                callback.accept(isCritical)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la vérification de la criticité de la mise à jour", e)
                callback.accept(false)
            }
    }

}
