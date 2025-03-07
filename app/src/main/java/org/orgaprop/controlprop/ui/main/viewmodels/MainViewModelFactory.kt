package org.orgaprop.controlprop.ui.main.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class MainViewModelFactory(private val loginRepository: LoginRepository, private val networkMonitor: NetworkMonitor) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(loginRepository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
