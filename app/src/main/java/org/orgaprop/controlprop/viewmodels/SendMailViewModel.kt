package org.orgaprop.controlprop.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.SendMailManager
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SendMailViewModel(
    private val sendMailManager: SendMailManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val TAG = "SendMailViewModel"
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private var idMbr: Int = -1
    private var adrMac: String = ""
    private var typeSend: String? = ""

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _sendResult = MutableStateFlow<Result<Unit>?>(null)
    val sendResult: StateFlow<Result<Unit>?> = _sendResult

    fun sendMail(
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String,
        message: String,
        photoUri: Uri? = null,
        currentPhotoPath: String? = null,
        entry: String?
    ) {
        viewModelScope.launch {
            try {
                // Vérifier la connectivité réseau
                if (!networkMonitor.isNetworkAvailable.value) {
                    _uiState.value = UiState.Error("Pas de connexion internet")
                    _sendResult.value = Result.failure(
                        BaseException(ErrorCodes.NETWORK_ERROR, "Pas de connexion internet")
                    )
                    return@launch
                }

                _uiState.value = UiState.Loading

                val result = kotlin.runCatching {
                    sendMailManager.sendMail(
                        idMbr = idMbr,
                        adrMac = adrMac,
                        typeSend = typeSend,
                        dest1 = dest1,
                        dest2 = dest2,
                        dest3 = dest3,
                        dest4 = dest4,
                        message = message,
                        photoUri = photoUri,
                        photoPath = currentPhotoPath,
                        entry = entry
                    )
                }

                result.fold(
                    onSuccess = { mailResult ->
                        mailResult.fold(
                            onSuccess = {
                                _uiState.value = UiState.Success("Mail envoyé avec succès")
                                _sendResult.value = Result.success(Unit)
                            },
                            onFailure = { e ->
                                handleError(e)
                            }
                        )
                    },
                    onFailure = { e ->
                        handleError(e)
                    }
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }



    private fun handleError(e: Throwable) {
        Log.e(TAG, "Error sending mail", e)

        val errorMessage = when (e) {
            is UnknownHostException -> "Serveur inaccessible"
            is SocketTimeoutException -> "Délai d'attente dépassé"
            is BaseException -> e.message ?: "Erreur ${e.code}"
            else -> e.message ?: "Erreur inconnue"
        }

        _uiState.value = UiState.Error(errorMessage)
        _sendResult.value = Result.failure(
            when (e) {
                is BaseException -> e
                else -> BaseException(ErrorCodes.UNKNOWN_ERROR, errorMessage, e)
            }
        )
    }



    fun setUserCredentials(idMbr: Int, adrMac: String, typeCtrl: String?) {
        Log.d(TAG, "setUserCredentials: idMbr: $idMbr, adrMac: $adrMac, typeCtrl: $typeCtrl")
        this.idMbr = idMbr
        this.adrMac = adrMac
        this.typeSend = typeCtrl
    }

}
