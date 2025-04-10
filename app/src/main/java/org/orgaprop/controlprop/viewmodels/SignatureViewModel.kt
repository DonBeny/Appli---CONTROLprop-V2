package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch

import org.orgaprop.controlprop.managers.SignatureManager
import org.orgaprop.controlprop.models.ObjSignature
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.LoginData

class SignatureViewModel(private val manager: SignatureManager) : ViewModel() {

    companion object {
        private const val TAG = "SignatureViewModel"
    }

    private lateinit var userData: LoginData
    private lateinit var entrySelected: SelectItem

    private val _signatureResult = MutableStateFlow<SignatureManager.SignatureResult?>(null)
    val signatureResult: StateFlow<SignatureManager.SignatureResult?> = _signatureResult.asStateFlow()

    private val _signatureState = MutableStateFlow<SignatureState>(SignatureState.Idle)
    val signatureState: StateFlow<SignatureState> = _signatureState.asStateFlow()



    fun setUserData(userData: LoginData) {
        this.userData = userData
    }
    fun setEntrySelected(currentEntry: SelectItem) {
        entrySelected = currentEntry
    }



    fun saveSignatures(entry: SelectItem, signatureData: ObjSignature) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Sauvegarde des signatures initiée pour l'entrée ${entry.id}")
                _signatureState.value = SignatureState.Loading

                val result = manager.saveSignatures(entry, signatureData)
                _signatureResult.value = result

                Log.d(TAG, "Résultat de la sauvegarde des signatures: $result")

                _signatureState.value = when(result) {
                    is SignatureManager.SignatureResult.Success -> SignatureState.Success
                    is SignatureManager.SignatureResult.PartialSuccess -> {
                        if (result.failedIds.contains(entry.id)) {
                            SignatureState.Error(Exception("Échec de synchronisation pour cet élément"))
                        } else {
                            SignatureState.Success
                        }
                    }
                    is SignatureManager.SignatureResult.Error ->
                        SignatureState.Error(Exception(result.message))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sauvegarde des signatures", e)
                _signatureState.value = SignatureState.Error(e)
                _signatureResult.value = SignatureManager.SignatureResult.Error("Une erreur inattendue s'est produite: ${e.message}")
            }
        }
    }

    fun clearResult() {
        _signatureResult.value = null
        _signatureState.value = SignatureState.Idle
    }



    sealed class SignatureState {
        data object Idle : SignatureState()
        data object Loading : SignatureState()
        data object Success : SignatureState()
        class Error(val exception: Throwable) : SignatureState()
    }

}
