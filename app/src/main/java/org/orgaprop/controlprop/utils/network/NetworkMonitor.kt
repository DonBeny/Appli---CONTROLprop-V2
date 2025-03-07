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

    /**
     * Démarre la surveillance du réseau.
     *
     * @param context Le contexte de l'application.
     */
    fun startMonitoring(context: Context) {
        Log.d(TAG, "startMonitoring")

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Crée une requête réseau pour surveiller les réseaux Wi-Fi et cellulaires
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Nécessaire pour accéder à Internet
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI) // Surveille le Wi-Fi
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR) // Surveille le réseau mobile
            .build()

        Log.d(TAG, "startMonitoring networkRequest: $networkRequest")

        // Enregistre le NetworkCallback pour surveiller les changements de réseau
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {

            // Appelé lorsqu'un réseau est disponible
            override fun onAvailable(network: Network) {
                Log.d(TAG, "network available")
                _isNetworkAvailable.value = true
            }

            // Appelé lorsqu'un réseau est perdu
            override fun onLost(network: Network) {
                Log.d(TAG, "network lost")
                _isNetworkAvailable.value = false
            }

            // Appelé lorsque les capacités du réseau changent (optionnel)
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidNetwork = hasInternet && (
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        )
                _isNetworkAvailable.value = isValidNetwork
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

}
