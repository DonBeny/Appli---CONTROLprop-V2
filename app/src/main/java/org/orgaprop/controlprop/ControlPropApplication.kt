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

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ControlPropApplication", "Uncaught exception in thread $thread", throwable)
        }

        startKoin {
            androidContext(this@ControlPropApplication)
            modules(appModule)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        val preferences = getSharedPreferences("ControlProp", MODE_PRIVATE)
        preferences.edit().remove("userData").apply()
    }

}
