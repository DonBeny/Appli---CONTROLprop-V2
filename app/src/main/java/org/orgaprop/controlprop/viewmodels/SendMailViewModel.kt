package org.orgaprop.controlprop.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.managers.SendMailManager

class SendMailViewModel(private val sendMailManager: SendMailManager) : ViewModel() {

    private val TAG = "SendMailViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""
    private var typeSend: String? = ""

    private val _sendResult = MutableStateFlow<Result<Unit>?>(null)
    val sendResult: StateFlow<Result<Unit>?> = _sendResult

    fun sendMail(
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String,
        message: String,
        bitmap: Bitmap?
    ) {
        viewModelScope.launch {
            _sendResult.value = kotlin.runCatching {
                sendMailManager.sendMail(
                    idMbr = idMbr,
                    adrMac = adrMac,
                    typeSend = typeSend,
                    dest1 = dest1,
                    dest2 = dest2,
                    dest3 = dest3,
                    dest4 = dest4,
                    message = message,
                    bitmap = bitmap
                )
            }
        }
    }



    fun setUserCredentials(idMbr: Int, adrMac: String, typeCtrl: String?) {
        Log.d(TAG, "setUserCredentials: idMbr: $idMbr, adrMac: $adrMac, typeCtrl: $typeCtrl")
        this.idMbr = idMbr
        this.adrMac = adrMac
        this.typeSend = typeCtrl
    }

}
