package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes

import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.ui.main.types.AgenceType
import org.orgaprop.controlprop.ui.main.types.InfoConf
import org.orgaprop.controlprop.ui.main.types.Limits
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.main.types.LoginError
import org.orgaprop.controlprop.ui.main.types.LoginResponse
import org.orgaprop.controlprop.ui.main.types.Rapport
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class MainViewModel(private val loginRepository: LoginRepository, private val networkMonitor: NetworkMonitor) : ViewModel() {

    private val TAG = "MainViewModel"

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

    // Fonction pour gérer la connexion
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Validation des entrées
                validateInputs(username, password)

                // Connexion via LoginRepository
                val responseJson = loginRepository.login(username, password)
                val response = parseLoginResponse(responseJson)

                if (response.status) {
                    _loginState.value = LoginState.Success(response.data!!)
                } else {
                    _loginState.value = LoginState.Error(response.error!!.txt)
                }
            } catch (e: BaseException) {
                _loginState.value = LoginState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    // Fonction pour gérer la déconnexion
    fun logout(idMbr: Int, adrMac: String) {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading
            try {
                val responseJson = loginRepository.logout(idMbr, adrMac)
                val response = parseLogoutResponse(responseJson)

                if (response.status) {
                    _logoutState.value = LogoutState.Success
                } else {
                    _logoutState.value = LogoutState.Error(response.error!!.txt)
                }
            } catch (e: BaseException) {
                _logoutState.value = LogoutState.Error(e.message ?: "Erreur de déconnexion")
            }
        }
    }

    fun checkLogin(username: String, password: String, adrMac: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val responseJson = loginRepository.checkLogin(username, password, adrMac)
                val response = parseLoginResponse(responseJson)

                if (response.status) {
                    _loginState.value = LoginState.Success(response.data!!)
                } else {
                    _loginState.value = LoginState.Error(response.error!!.txt)
                }
            } catch (e: BaseException) {
                _loginState.value = LoginState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    private fun parseLoginResponse(responseJson: JSONObject): LoginResponse {
        return try {
            Log.d(TAG, "parseLoginResponse: Réponse JSON: $responseJson")

            val status = responseJson.getBoolean("status")

            Log.d(TAG, "parseLoginResponse: Status: $status")

            if (status) {
                val data = responseJson.getJSONObject("data")

                Log.d(TAG, "parseLoginResponse: Data: $data")

                LoginResponse(
                    status = true,
                    data = LoginData(
                        agences = parseAgences(data.getJSONArray("agences")),
                        version = data.getInt("version"),
                        idMbr = data.getInt("idMbr"),
                        adrMac = data.getString("adrMac"),
                        hasContrat = data.getBoolean("hasContrat"),
                        info = parseInfoConf(data.getJSONObject("info")),
                        limits = parseLimits(data.getJSONObject("limits")),
                        planActions = data.getString("planActions")
                    )
                )
            } else {
                val error = responseJson.getJSONObject("error")

                Log.d(TAG, "parseLoginResponse: error: $error")

                LoginResponse(
                    status = false,
                    error = LoginError(
                        code = error.getInt("code"),
                        txt = error.getString("txt")
                    )
                )
            }
        } catch (e: JSONException) {
            throw BaseException(ErrorCodes.INVALID_RESPONSE, "Réponse JSON invalide", e)
        }
    }

    private fun parseLogoutResponse(responseJson: JSONObject): LoginResponse {
        return try {
            Log.d(TAG, "parseLogoutResponse: Réponse JSON: $responseJson")

            val status = responseJson.getBoolean("status")

            Log.d(TAG, "parseLogoutResponse: Status: $status")

            if (status) {
                LoginResponse(status = true)
            } else {
                val error = responseJson.getJSONObject("error")

                Log.d(TAG, "parseLogoutResponse: error: $error")

                LoginResponse(
                    status = false,
                    error = LoginError(
                        code = error.getInt("code"),
                        txt = error.getString("txt")
                        )
                )
            }
        } catch (e: JSONException) {
            throw BaseException(ErrorCodes.INVALID_RESPONSE, "Réponse JSON invalide", e)
        }
    }

    private fun parseAgences(agencesArray: JSONArray): List<AgenceType> {
        val agences = mutableListOf<AgenceType>()
        for (i in 0 until agencesArray.length()) {
            val agence = agencesArray.getJSONObject(i)
            agences.add(
                AgenceType(
                    id = agence.getInt("id"),
                    nom = agence.getString("nom"),
                    tech = agence.getString("tech"),
                    contact = agence.getString("contact")
                )
            )
        }
        return agences
    }

    private fun parseInfoConf(infoObject: JSONObject): InfoConf {
        return InfoConf(
            aff = infoObject.getString("aff"),
            prod = infoObject.getString("prod")
        )
    }

    private fun parseLimits(limitsObject: JSONObject): Limits {
        return Limits(
            top = limitsObject.getInt("top"),
            down = limitsObject.getInt("down"),
            rapport = parseRapport(limitsObject.getJSONObject("rapport"))
        )
    }

    private fun parseRapport(rapportObject: JSONObject): Rapport {
        return Rapport(
            value = rapportObject.getInt("value"),
            dest = rapportObject.getString("dest")
        )
    }

    fun checkVersion(idMbr: String, deviceId: String) {
        viewModelScope.launch {
            _versionState.value = VersionState.Loading
            try {
                val response = loginRepository.checkVersion(idMbr, deviceId)
                _versionState.value = VersionState.Success(response)
            } catch (e: BaseException) {
                _versionState.value = VersionState.Error(e.message ?: "Version check failed")
            }
        }
    }

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

    fun updateTheme(isDarkMode: Boolean) {
        _uiState.value = UiState.ThemeChanged(isDarkMode)
    }

    fun updateLayout(newLayout: Int, oldLayout: Int) {
        _uiState.value = UiState.LayoutChanged(newLayout, oldLayout)
    }

    fun clearLoginData() {
        loginRepository.clearLoginData()
    }

    // États de connexion
    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val data: LoginData) : LoginState()
        object LoggedOut : LoginState()
        data class Error(val message: String) : LoginState()
    }

    // États de déconnexion
    sealed class LogoutState {
        object Loading : LogoutState()
        object Success : LogoutState()
        data class Error(val message: String) : LogoutState()
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
    class ValidationException(message: String) : BaseException(ErrorCodes.INVALID_RESPONSE, message)

}
