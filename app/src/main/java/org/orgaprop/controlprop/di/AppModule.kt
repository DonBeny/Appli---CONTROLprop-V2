package org.orgaprop.controlprop.di

import android.content.Context
import android.util.Log

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.orgaprop.controlprop.managers.GetMailManager

import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.managers.SelectEntryManager
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.viewmodels.MainViewModel
import org.orgaprop.controlprop.viewmodels.GetMailViewModel
import org.orgaprop.controlprop.viewmodels.SelectEntryViewModel
import org.orgaprop.controlprop.viewmodels.SelectListViewModel

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

    single {
        Log.e("AppModule", "Creating GetMailManager...")
        GetMailManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating SelectListManager...")
        SelectEntryManager(get(), get())
    }
    // Déclarer SelectListManager comme un singleton
    single {
        Log.e("AppModule", "Creating SelectListManager...")
        SelectListManager(get(), get())
    }


    // Définir MainViewModel comme une dépendance injectable
    viewModel {
        Log.e("AppModule", "Creating MainViewModel...")
        MainViewModel(get(), get())
    }

    viewModel {
        Log.e("AppModule", "Creating GetMailViewModel...")
        GetMailViewModel(get(), get())
    }

    viewModel {
        Log.e("AppModule", "Creating SelectEntryViewModel...")
        SelectEntryViewModel(get())
    }

    // Déclarer SelectListViewModel comme une dépendance injectable
    viewModel {
        Log.e("AppModule", "Creating SelectListViewModel...")
        SelectListViewModel(get())
    }

}
