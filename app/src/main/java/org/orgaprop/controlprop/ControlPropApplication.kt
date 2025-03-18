package org.orgaprop.controlprop

import android.app.Application
import android.util.Log

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import org.orgaprop.controlprop.di.appModule

class ControlPropApplication : Application(), DefaultLifecycleObserver {

    private val TAG = "ControlPropApplication"

    override fun onCreate() {
        super<Application>.onCreate()

        Log.d(TAG, "onCreate: Application started")

        // Démarrer Koin
        startKoin {
            androidContext(this@ControlPropApplication)
            modules(appModule)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "onStop: Application en arrière-plan")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "onDestroy: Application fermée")

        // Supprimer userData lorsque l'application est fermée
        val preferences = getSharedPreferences("ControlProp", MODE_PRIVATE)
        preferences.edit().remove("userData").apply()
    }

}
