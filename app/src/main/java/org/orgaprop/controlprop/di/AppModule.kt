package org.orgaprop.controlprop.di

import android.content.Context
import android.util.Log

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import org.orgaprop.controlprop.ui.main.repository.LoginRepository
import org.orgaprop.controlprop.managers.GetMailManager
import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.managers.PlanActionsManager
import org.orgaprop.controlprop.managers.SelectEntryManager
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.managers.TypeCtrlManager
import org.orgaprop.controlprop.viewmodels.ConfigCtrlViewModel
import org.orgaprop.controlprop.viewmodels.MainViewModel
import org.orgaprop.controlprop.viewmodels.GetMailViewModel
import org.orgaprop.controlprop.viewmodels.PlanActionsViewModel
import org.orgaprop.controlprop.viewmodels.SelectEntryViewModel
import org.orgaprop.controlprop.viewmodels.SelectListViewModel
import org.orgaprop.controlprop.viewmodels.TypeCtrlViewModel


val viewModelModule = module {

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

    viewModel {
        Log.e("AppModule", "Creating SelectListViewModel...")
        SelectListViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating TypeCtrlViewModel...")
        TypeCtrlViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating PlanActionsViewModel...")
        PlanActionsViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating ConfigCtrlViewModel...")
        ConfigCtrlViewModel()
    }

}

val managerModule = module {

    single {
        Log.e("AppModule", "Creating HttpTask...")
        HttpTask(get())
    }

    single {
        Log.e("AppModule", "Creating LoginManager...")
        LoginManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating LoginRepository...")
        LoginRepository(get())
    }

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
        Log.e("AppModule", "Creating SelectEntryManager...")
        SelectEntryManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating SelectListManager...")
        SelectListManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating TypeCtrlManager...")
        TypeCtrlManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating PlanActionsManager...")
        PlanActionsManager(get(), get())
    }

}

val appModule = listOf(viewModelModule, managerModule)
