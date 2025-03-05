package org.orgaprop.controlprop.ui.main.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch

import org.json.JSONObject

import org.orgaprop.controlprop.ui.main.repository.LoginRepository

class MainViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    // États de connexion
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> get() = _loginState

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

    // Fonction pour gérer la connexion
    fun login(username: String, password: String, remember: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Validation des entrées
                validateInputs(username, password)

                // Connexion via LoginRepository
                val response = loginRepository.login(username, password, remember)
                _loginState.value = LoginState.Success(response)
            } catch (e: LoginRepository.LoginException) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    // Fonction pour gérer la déconnexion
    fun logout(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = loginRepository.logout(username, password)
                _loginState.value = LoginState.LoggedOut
            } catch (e: LoginRepository.LoginException) {
                _loginState.value = LoginState.Error(e.message ?: "Logout failed")
            }
        }
    }

    // Fonction pour vérifier la version de l'application
    fun checkVersion(idMbr: String, deviceId: String) {
        viewModelScope.launch {
            _versionState.value = VersionState.Loading
            try {
                val response = loginRepository.checkVersion(idMbr, deviceId)
                _versionState.value = VersionState.Success(response)
            } catch (e: LoginRepository.LoginException) {
                _versionState.value = VersionState.Error(e.message ?: "Version check failed")
            }
        }
    }

    // Fonction pour valider les entrées en temps réel
    fun validateInputs(username: String, password: String) {
        try {
            // Validation des entrées
            if (username.isEmpty() || password.isEmpty()) {
                throw ValidationException("Username and password cannot be empty")
            }
            _validationState.value = ValidationState.Valid
        } catch (e: ValidationException) {
            _validationState.value = ValidationState.Invalid(e.message ?: "Invalid input")
        }
    }

    // Fonction pour gérer les changements de thème
    fun updateTheme(isDarkMode: Boolean) {
        _uiState.value = UiState.ThemeChanged(isDarkMode)
    }

    // Fonction pour gérer les changements de layout
    fun updateLayout(newLayout: Int, oldLayout: Int) {
        _uiState.value = UiState.LayoutChanged(newLayout, oldLayout)
    }

    // Fonction pour nettoyer les données de connexion
    fun clearLoginData() {
        loginRepository.clearLoginData()
    }

    // États de connexion
    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val response: JSONObject) : LoginState()
        object LoggedOut : LoginState()
        data class Error(val message: String) : LoginState()
    }

    // États de validation des entrées
    sealed class ValidationState {
        object Valid : ValidationState()
        data class Invalid(val message: String) : ValidationState()
    }

    // États de permission
    sealed class PermissionState {
        object Granted : PermissionState()
        data class Denied(val message: String) : PermissionState()
        data class Error(val message: String) : PermissionState()
    }

    // États de version
    sealed class VersionState {
        object Loading : VersionState()
        data class Success(val response: JSONObject) : VersionState()
        data class Error(val message: String) : VersionState()
    }

    // États de l'interface utilisateur
    sealed class UiState {
        data class ThemeChanged(val isDarkMode: Boolean) : UiState()
        data class LayoutChanged(val newLayout: Int, val oldLayout: Int) : UiState()
    }

    // Exception personnalisée pour les erreurs de validation
    class ValidationException(message: String) : Exception(message)

}
