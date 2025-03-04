package org.orgaprop.controlprop.managers

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.util.function.Consumer

/**
 * Gestionnaire des permissions de l'application.
 */
class PermissionManager(private val activity: AppCompatActivity) {

    companion object {
        private const val TAG = "PermissionManager"
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionCallback: Consumer<Boolean>? = null

    // Permissions requises pour l'application
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    // Permissions spécifiques à l'activité principale
    private val MAIN_ACTIVITY_PERMISSIONS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    init {
        initializePermissionLauncher()
    }

    /**
     * Initialise le launcher pour la demande de permissions.
     */
    private fun initializePermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = result.values.all { it }
            permissionCallback?.accept(allGranted)
            Log.d(TAG, "Permissions ${if (allGranted) "accordées" else "refusées"}")
        }
    }

    /**
     * Interface pour les callbacks de résultat des permissions.
     */
    interface PermissionResultCallback {
        fun onResult(granted: Boolean)
        fun onError(e: Exception) {
            Log.e(TAG, "Erreur lors de la demande de permissions", e)
        }
    }

    /**
     * Vérifie et demande les permissions requises.
     *
     * @param callback Callback appelé avec le résultat.
     * @return true si toutes les permissions sont déjà accordées.
     */
    fun checkRequiredPermissions(callback: PermissionResultCallback): Boolean {
        return try {
            val permissionsToRequest = getPermissionsToRequest(REQUIRED_PERMISSIONS)

            if (permissionsToRequest.isEmpty()) {
                callback.onResult(true)
                true
            } else {
                requestPermissions(permissionsToRequest, object : PermissionResultCallback {
                    override fun onResult(granted: Boolean) {
                        callback.onResult(granted)
                    }

                    override fun onError(e: Exception) {
                        val baseException = if (e is BaseException) e else BaseException(
                            ErrorCodes.PERMISSION_ERROR,
                            "Erreur lors de la demande de permissions",
                            e
                        )
                        callback.onError(baseException)
                    }
                })
                false
            }
        } catch (e: Exception) {
            val baseException = if (e is BaseException) e else BaseException(
                ErrorCodes.PERMISSION_ERROR,
                "Erreur lors de la vérification des permissions",
                e
            )
            callback.onError(baseException)
            false
        }
    }

    /**
     * Vérifie et demande les permissions spécifiques à l'activité principale.
     *
     * @param callback Callback appelé avec le résultat.
     * @return true si toutes les permissions sont déjà accordées.
     */
    fun checkMainActivityPermissions(callback: PermissionResultCallback): Boolean {
        return try {
            val permissionsToRequest = getPermissionsToRequest(MAIN_ACTIVITY_PERMISSIONS)

            if (permissionsToRequest.isEmpty()) {
                callback.onResult(true)
                true
            } else {
                requestPermissions(permissionsToRequest, object : PermissionResultCallback {
                    override fun onResult(granted: Boolean) {
                        if (!granted) {
                            handleDeniedPermissions(permissionsToRequest)
                        }
                        callback.onResult(granted)
                    }

                    override fun onError(e: Exception) {
                        val baseException = if (e is BaseException) e else BaseException(
                            ErrorCodes.PERMISSION_ERROR,
                            "Erreur lors de la demande de permissions",
                            e
                        )
                        callback.onError(baseException)
                    }
                })
                false
            }
        } catch (e: Exception) {
            val baseException = if (e is BaseException) e else BaseException(
                ErrorCodes.PERMISSION_ERROR,
                "Erreur lors de la vérification des permissions",
                e
            )
            callback.onError(baseException)
            false
        }
    }

    /**
     * Gère les permissions refusées.
     *
     * @param deniedPermissions Liste des permissions refusées.
     */
    private fun handleDeniedPermissions(deniedPermissions: List<String>) {
        for (permission in deniedPermissions) {
            when (permission) {
                Manifest.permission.INTERNET -> activity.finish() // Permission Internet obligatoire
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> {
                    Log.w(TAG, "Permissions calendrier refusées")
                }
            }
        }
    }

    /**
     * Retourne la liste des permissions à demander.
     *
     * @param permissions Liste des permissions à vérifier.
     * @return Liste des permissions non accordées.
     */
    private fun getPermissionsToRequest(permissions: Array<String>): List<String> {
        return permissions.filter { !isPermissionGranted(it) }
    }

    /**
     * Demande les permissions spécifiées.
     *
     * @param permissions Permissions à demander.
     * @param callback Callback appelé avec le résultat.
     */
    private fun requestPermissions(permissions: List<String>, callback: PermissionResultCallback) {
        permissionCallback = Consumer { granted ->
            try {
                callback.onResult(granted)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    /**
     * Vérifie si une permission spécifique est accordée.
     *
     * @param permission Permission à vérifier.
     * @return true si la permission est accordée.
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Groupe les permissions requises par catégorie.
     */
    enum class PermissionGroup(vararg permissions: String) {
        STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        CALENDAR(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
        NETWORK(Manifest.permission.INTERNET);

        val permissions: Array<out String> = permissions
    }

    /**
     * Vérifie si toutes les permissions d'un groupe sont accordées.
     *
     * @param group Groupe de permissions à vérifier.
     * @return true si toutes les permissions du groupe sont accordées.
     */
    fun areGroupPermissionsGranted(group: PermissionGroup): Boolean {
        return group.permissions.all { isPermissionGranted(it) }
    }

    /**
     * Demande uniquement les permissions d'un groupe spécifique.
     *
     * @param group Groupe de permissions à demander.
     * @param callback Callback appelé avec le résultat.
     */
    fun requestGroupPermissions(group: PermissionGroup, callback: Consumer<Boolean>) {
        val permissionsToRequest = group.permissions.filter { !isPermissionGranted(it) }

        if (permissionsToRequest.isEmpty()) {
            callback.accept(true)
            return
        }

        permissionCallback = callback
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

}
