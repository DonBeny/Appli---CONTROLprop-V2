package org.orgaprop.controlprop.di

import android.content.Context
import android.util.Log

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.ui.main.viewmodels.MainViewModel
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.NetworkMonitor

val appModule = module {

    single {
        Log.e("AppModule", "Creating HttpTask...")
        HttpTask(get())
    }

    single {
        Log.e("AppModule", "Creating LoginManager...")
        LoginManager(get(), get())
    }

    // Définir LoginRepository comme un singleton
    single {
        Log.e("AppModule", "Creating LoginRepository...")
        LoginRepository(get())
    }

    // Démarrer la surveillance du réseau au démarrage de l'application
    single {
        Log.e("AppModule", "Starting network monitoring...")
        val context = get<Context>()
        NetworkMonitor.startMonitoring(context)
        NetworkMonitor
    }

    // Définir MainViewModel comme une dépendance injectable
    viewModel {
        Log.e("AppModule", "Creating MainViewModel...")
        MainViewModel(get(), get())
    }

}
