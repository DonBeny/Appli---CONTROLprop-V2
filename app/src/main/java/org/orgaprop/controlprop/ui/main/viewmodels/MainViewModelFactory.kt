package org.orgaprop.controlprop.ui.main.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.orgaprop.controlprop.ui.main.repository.LoginRepository

class MainViewModelFactory(private val loginRepository: LoginRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
