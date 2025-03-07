package org.orgaprop.controlprop

import android.app.Application
import android.util.Log

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import org.orgaprop.controlprop.di.appModule

class ControlPropApplication : Application() {

    private val TAG = "ControlPropApplication"

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate: Application started")

        // Démarrer Koin
        startKoin {
            androidContext(this@ControlPropApplication)
            modules(appModule)
        }
    }

}
