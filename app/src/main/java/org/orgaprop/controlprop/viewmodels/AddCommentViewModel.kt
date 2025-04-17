package org.orgaprop.controlprop.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.FileUtils
import org.orgaprop.controlprop.utils.LogUtils

class AddCommentViewModel : ViewModel() {

    companion object {
        const val TAG = "AddCommentViewModel"
    }

    private lateinit var userData: LoginData

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    fun setUserData(user: LoginData) {
        this.userData = user
    }

    fun setCommentText(text: String) {
        _commentText.value = text
    }
    fun updateCommentText(text: String) {
        if (_commentText.value != text) {
            _commentText.value = text
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        _imageBitmap.value = bitmap
    }

    fun prepareCommentData() {
        LogUtils.d(TAG, "prepareCommentData: Comment text: ${_commentText.value}")
        LogUtils.d(TAG, "prepareCommentData: Image bitmap: ${_imageBitmap.value}")

        _navigationEvent.value = NavigationEvent.SaveComment(
            commentText = _commentText.value,
            imageBase64 = _imageBitmap.value?.let { FileUtils.bitmapToBase64(it) } ?: ""
        )
    }

    fun cancelComment() {
        _navigationEvent.value = NavigationEvent.Cancel
    }

    sealed class NavigationEvent {
        data class SaveComment(val commentText: String, val imageBase64: String) : NavigationEvent()
        data object Cancel : NavigationEvent()
    }

}