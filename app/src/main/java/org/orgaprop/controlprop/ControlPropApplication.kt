package org.orgaprop.controlprop

import android.app.Application
import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.ui.main.repository.LoginRepository

class ControlPropApplication : Application() {

    // Instance unique de LoginRepository
    val loginRepository: LoginRepository by lazy {
        LoginRepository(LoginManager.getInstance(this))
    }

    override fun onCreate() {
        super.onCreate()
        // Vous pouvez initialiser d'autres dépendances globales ici si nécessaire
    }

}
