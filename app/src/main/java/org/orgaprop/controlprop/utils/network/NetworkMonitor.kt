package org.orgaprop.controlprop.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Classe pour surveiller la connectivité réseau en temps réel.
 * Utilise `ConnectivityManager.NetworkCallback` pour détecter les changements de réseau.
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    // StateFlow pour exposer l'état du réseau (true = réseau disponible, false = réseau indisponible)
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    // Callbacks
    private var networkCallbacks = mutableListOf<NetworkCallback>()

    /**
     * Interface pour notifier les observateurs des changements d'état du réseau
     */
    interface NetworkCallback {
        fun onNetworkStateChanged(isConnected: Boolean)
    }

    /**
     * Ajoute un callback pour être notifié des changements d'état du réseau
     */
    fun setNetworkCallback(callback: NetworkCallback) {
        if (!networkCallbacks.contains(callback)) {
            networkCallbacks.add(callback)

            // Notifier immédiatement de l'état actuel
            callback.onNetworkStateChanged(_isNetworkAvailable.value)
        }
    }

    /**
     * Supprime un callback
     */
    fun removeNetworkCallback(callback: NetworkCallback) {
        networkCallbacks.remove(callback)
    }

    /**
     * Vérifie si le réseau est actuellement connecté
     * @return true si le réseau est disponible, false sinon
     */
    fun isConnected(): Boolean {
        return _isNetworkAvailable.value
    }

    /**
     * Démarre la surveillance du réseau.
     *
     * @param context Le contexte de l'application.
     */
    fun startMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Vérifier l'état initial du réseau
        updateConnectionState(connectivityManager)

        // Crée une requête réseau pour surveiller les réseaux Wi-Fi et cellulaires
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        // Enregistre le NetworkCallback pour surveiller les changements de réseau
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            // Appelé lorsqu'un réseau est disponible
            override fun onAvailable(network: Network) {
                updateNetworkState(true)
            }

            // Appelé lorsqu'un réseau est perdu
            override fun onLost(network: Network) {
                updateNetworkState(false)
            }

            // Appelé lorsque les capacités du réseau changent
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidNetwork = hasInternet && (
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        )
                updateNetworkState(isValidNetwork)
            }
        })
    }

    /**
     * Arrête la surveillance du réseau.
     *
     * @param context Le contexte de l'application.
     */
    fun stopMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(object : ConnectivityManager.NetworkCallback() {})
    }

    /**
     * Vérifie l'état actuel de la connexion réseau
     */
    private fun updateConnectionState(connectivityManager: ConnectivityManager) {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            val isConnected = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

            updateNetworkState(isConnected)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification de l'état du réseau", e)
            updateNetworkState(false)
        }
    }

    /**
     * Met à jour l'état du réseau et notifie les observateurs
     */
    private fun updateNetworkState(isConnected: Boolean) {
        if (_isNetworkAvailable.value != isConnected) {
            Log.d(TAG, "Network state changed: ${if (isConnected) "CONNECTED" else "DISCONNECTED"}")
            _isNetworkAvailable.value = isConnected

            // Notifier tous les callbacks
            networkCallbacks.forEach { callback ->
                callback.onNetworkStateChanged(isConnected)
            }
        }
    }
}