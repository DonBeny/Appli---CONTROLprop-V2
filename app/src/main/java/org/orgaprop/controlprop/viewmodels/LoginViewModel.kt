package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.login.mappers.LoginResponseMapper
import org.orgaprop.controlprop.ui.login.repository.LoginRepository
import org.orgaprop.controlprop.ui.login.states.LoginState
import org.orgaprop.controlprop.ui.login.states.LogoutState
import org.orgaprop.controlprop.ui.login.states.PermissionState
import org.orgaprop.controlprop.ui.login.states.UiState
import org.orgaprop.controlprop.ui.login.states.ValidationState
import org.orgaprop.controlprop.ui.login.states.VersionState
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val TAG = "LoginViewModel"
    private val responseMapper = LoginResponseMapper()

    // États de connexion
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> get() = _loginState

    private val _logoutState = MutableLiveData<LogoutState>()
    val logoutState: LiveData<LogoutState> get() = _logoutState

    // États de validation des entrées
    private val _validationState = MutableLiveData<ValidationState>()
    val validationState: LiveData<ValidationState> get() = _validationState

    // États de permission
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> get() = _permissionState

    // États de version
    private val _versionState = MutableLiveData<VersionState>()
    val versionState: LiveData<VersionState> get() = _versionState

    // États de l'interface utilisateur
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> get() = _uiState

    // État du réseau
    private val _networkState = MutableLiveData<Boolean>()
    val networkState: LiveData<Boolean> get() = _networkState

    // Callback pour le NetworkMonitor
    private val networkCallback = object : NetworkMonitor.NetworkCallback {
        override fun onNetworkStateChanged(isConnected: Boolean) {
            _networkState.postValue(isConnected)
        }
    }

    init {
        // S'inscrire pour recevoir les mises à jour de l'état du réseau
        networkMonitor.setNetworkCallback(networkCallback)
        _networkState.value = networkMonitor.isConnected()
    }

    override fun onCleared() {
        super.onCleared()
        // Se désinscrire du NetworkMonitor
        networkMonitor.removeNetworkCallback(networkCallback)
    }

    /**
     * Fonction pour gérer la connexion
     */
    fun login(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.postValue(LoginState.Loading)
            try {
                // Validation des entrées
                validateInputs(username, password)

                // Vérifier la connexion réseau
                if (!networkMonitor.isConnected()) {
                    _loginState.postValue(LoginState.Error("Aucune connexion réseau disponible"))
                    return@launch
                }

                // Connexion via LoginRepository
                val responseJson = loginRepository.login(username, password)
                val response = responseMapper.parseLoginResponse(responseJson)

                if (response.status) {
                    _loginState.postValue(LoginState.Success(response.data!!))
                } else {
                    _loginState.postValue(LoginState.Error(response.error!!.txt))
                }
            } catch (e: ValidationException) {
                _loginState.postValue(LoginState.Error(e.message ?: "Erreur de validation"))
            } catch (e: BaseException) {
                _loginState.postValue(LoginState.Error(getDetailedErrorMessage(e)))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors de la connexion", e)
                _loginState.postValue(LoginState.Error("Une erreur inattendue s'est produite: ${e.message}"))
            }
        }
    }

    /**
     * Fonction pour gérer la déconnexion
     */
    fun logout(idMbr: Int, adrMac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _logoutState.postValue(LogoutState.Loading)
            try {
                // Vérifier la connexion réseau
                if (!networkMonitor.isConnected()) {
                    _logoutState.postValue(LogoutState.Error("Aucune connexion réseau disponible"))
                    return@launch
                }

                val responseJson = loginRepository.logout(idMbr, adrMac)
                val response = responseMapper.parseLogoutResponse(responseJson)

                if (response.status) {
                    _logoutState.postValue(LogoutState.Success)
                } else {
                    _logoutState.postValue(LogoutState.Error(response.error!!.txt))
                }
            } catch (e: BaseException) {
                _logoutState.postValue(LogoutState.Error(getDetailedErrorMessage(e)))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors de la déconnexion", e)
                _logoutState.postValue(LogoutState.Error("Une erreur inattendue s'est produite: ${e.message}"))
            }
        }
    }

    /**
     * Vérifier les identifiants de connexion sans se connecter complètement
     */
    fun checkLogin(username: String, password: String, adrMac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.postValue(LoginState.Loading)
            try {
                // Vérifier la connexion réseau
                if (!networkMonitor.isConnected()) {
                    _loginState.postValue(LoginState.Error("Aucune connexion réseau disponible"))
                    return@launch
                }

                val responseJson = loginRepository.checkLogin(username, password, adrMac)
                val response = responseMapper.parseLoginResponse(responseJson)

                if (response.status) {
                    _loginState.postValue(LoginState.Success(response.data!!))
                } else {
                    _loginState.postValue(LoginState.Error(response.error!!.txt))
                }
            } catch (e: BaseException) {
                _loginState.postValue(LoginState.Error(getDetailedErrorMessage(e)))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors de la vérification de connexion", e)
                _loginState.postValue(LoginState.Error("Une erreur inattendue s'est produite: ${e.message}"))
            }
        }
    }

    /**
     * Obtient un message d'erreur détaillé pour une BaseException
     */
    private fun getDetailedErrorMessage(e: BaseException): String {
        val baseMessage = e.message ?: "Erreur inconnue"

        return when (e.code) {
            ErrorCodes.NETWORK_ERROR -> "$baseMessage. Vérifiez votre connexion internet."
            ErrorCodes.INVALID_RESPONSE -> "$baseMessage. Le serveur a retourné une réponse invalide."
            ErrorCodes.LOGIN_FAILED -> "$baseMessage. Vérifiez vos identifiants."
            ErrorCodes.LOGOUT_FAILED -> "$baseMessage. Impossible de vous déconnecter correctement."
            else -> baseMessage
        }
    }

    /**
     * Valide les informations d'identification
     */
    fun validateInputs(username: String, password: String) {
        try {
            when {
                username.isEmpty() && password.isEmpty() -> throw ValidationException("Le nom d'utilisateur et le mot de passe sont requis")
                username.isEmpty() -> throw ValidationException("Le nom d'utilisateur est requis")
                password.isEmpty() -> throw ValidationException("Le mot de passe est requis")
                username.length < 3 -> throw ValidationException("Le nom d'utilisateur doit contenir au moins 3 caractères")
                password.length < 6 -> throw ValidationException("Le mot de passe doit contenir au moins 6 caractères")
            }
            _validationState.postValue(ValidationState.Valid)
        } catch (e: ValidationException) {
            _validationState.postValue(ValidationState.Invalid(e.message ?: "Entrée invalide"))
            throw e // Relancer pour être attrapée par la méthode appelante
        }
    }

    /**
     * Met à jour le thème de l'application
     */
    fun updateTheme(isDarkMode: Boolean) {
        _uiState.value = UiState.ThemeChanged(isDarkMode)
    }

    /**
     * Met à jour la disposition de l'écran
     */
    fun updateLayout(newLayout: Int, oldLayout: Int) {
        _uiState.value = UiState.LayoutChanged(newLayout, oldLayout)
    }

    /**
     * Efface les données de connexion
     */
    fun clearLoginData() {
        loginRepository.clearLoginData()
    }

    /**
     * Exception personnalisée pour les erreurs de validation
     */
    class ValidationException(message: String) : BaseException(ErrorCodes.INVALID_INPUT, message)
}