package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.GetMailManager
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import java.io.IOException

class GetMailViewModel(
    private val getMailManager: GetMailManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val TAG = "GetMailViewModel"
    }

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> get() = _response

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading



    /**
     * Valide et envoie l'adresse email au serveur pour récupérer les identifiants.
     *
     * @param email L'adresse email à envoyer
     * @return true si la validation est passée et que la requête a été lancée, false sinon
     */
    fun submitEmail(email: String): Boolean {
        if (!isValidEmail(email)) {
            _error.value = "Adresse email invalide"
            return false
        }

        if (!networkMonitor.isConnected()) {
            _error.value = ErrorCodes.getMessageForCode(ErrorCodes.NETWORK_ERROR)
            return false
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = getMailManager.submitEmail(email)

                if (result.has("status") && result.getBoolean("status")) {
                    _response.value = "Vos identifiants ont été envoyés à votre adresse email"
                    Log.d(TAG, "Identifiants envoyés avec succès à $email")
                } else {
                    val message = result.optString("message", "Une erreur est survenue")
                    _response.value = message

                    if (result.has("error")) {
                        _error.value = result.getString("error")
                        Log.w(TAG, "Erreur de récupération des identifiants: ${result.getString("error")}")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Erreur réseau lors de la récupération des identifiants", e)
                _error.value = ErrorCodes.getMessageForCode(ErrorCodes.NETWORK_ERROR)
            } catch (e: BaseException) {
                Log.e(TAG, "Erreur lors de la récupération des identifiants", e)
                _error.value = ErrorCodes.getMessageForCode(e.code)
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la réponse", e)
                _error.value = ErrorCodes.getMessageForCode(ErrorCodes.INVALID_RESPONSE)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inconnue lors de la récupération des identifiants", e)
                _error.value = ErrorCodes.getMessageForCode(ErrorCodes.UNKNOWN_ERROR)
            } finally {
                _isLoading.value = false
            }
        }
        return true
    }

    /**
     * Vérifie si l'adresse email est valide selon le format standard.
     *
     * @param email L'adresse email à valider
     * @return true si l'email est valide, false sinon
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}
