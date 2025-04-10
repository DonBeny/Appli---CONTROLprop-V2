package org.orgaprop.controlprop.di

import android.content.Context
import android.util.Log

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.orgaprop.controlprop.managers.CtrlZoneManager
import org.orgaprop.controlprop.managers.FinishCtrlManager

import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.NetworkMonitor
import org.orgaprop.controlprop.ui.login.repository.LoginRepository
import org.orgaprop.controlprop.managers.GetMailManager
import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.managers.LoginManager
import org.orgaprop.controlprop.managers.PlanActionsManager
import org.orgaprop.controlprop.managers.SelectEntryManager
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.managers.SendMailManager
import org.orgaprop.controlprop.managers.SignatureManager
import org.orgaprop.controlprop.managers.TypeCtrlManager
import org.orgaprop.controlprop.security.SecureCredentialsManager
import org.orgaprop.controlprop.sync.SyncManager
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.login.mappers.LoginResponseMapper
import org.orgaprop.controlprop.viewmodels.MainViewModel
import org.orgaprop.controlprop.viewmodels.AddCommentViewModel
import org.orgaprop.controlprop.viewmodels.ConfigCtrlViewModel
import org.orgaprop.controlprop.viewmodels.CtrlZoneViewModel
import org.orgaprop.controlprop.viewmodels.FinishCtrlViewModel
import org.orgaprop.controlprop.viewmodels.LoginViewModel
import org.orgaprop.controlprop.viewmodels.GetMailViewModel
import org.orgaprop.controlprop.viewmodels.GrilleCtrlViewModel
import org.orgaprop.controlprop.viewmodels.PlanActionsViewModel
import org.orgaprop.controlprop.viewmodels.SelectEntryViewModel
import org.orgaprop.controlprop.viewmodels.SelectListViewModel
import org.orgaprop.controlprop.viewmodels.SendMailViewModel
import org.orgaprop.controlprop.viewmodels.SignatureViewModel
import org.orgaprop.controlprop.viewmodels.TypeCtrlViewModel



val viewModelModule = module {
    viewModel {
        Log.e("AppModule", "Creating MainViewModel...")
        MainViewModel()
    }

    viewModel {
        Log.e("AppModule", "Creating LoginViewModel...")
        LoginViewModel(get(), get())
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
        PlanActionsViewModel(get(), get())
    }

    viewModel {
        Log.e("AppModule", "Creating ConfigCtrlViewModel...")
        ConfigCtrlViewModel()
    }

    viewModel {
        Log.e("AppModule", "Creating GrilleCtrlViewModel...")
        GrilleCtrlViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating SendMailViewModel...")
        SendMailViewModel(get(), get())
    }

    viewModel {
        Log.e("AppModule", "Creating CtrlZoneViewModel...")
        CtrlZoneViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating AddCommentViewModel...")
        AddCommentViewModel()
    }

    viewModel {
        Log.e("AppModule", "Creating FinishCtrlViewModel...")
        FinishCtrlViewModel(get())
    }

    viewModel {
        Log.e("AppModule", "Creating SignatureViewModel...")
        SignatureViewModel(get())
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
        Log.e("AppModule", "Creating LoginResponseMapper...")
        LoginResponseMapper()
    }

    single {
        Log.e("AppModule", "Creating SecureCredentialsManager...")
        SecureCredentialsManager(get())
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

    single {
        Log.e("AppModule", "Creating GrilleCtrlManager...")
        GrilleCtrlManager(get(), get(), get())
    }

    single {
        Log.e("AppModule", "Creating CtrlZoneManager...")
        CtrlZoneManager(get())
    }

    single {
        Log.e("AppModule", "Creating SendMailManager...")
        SendMailManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating SyncManager...")
        SyncManager(get(), get(), get())
    }

    single {
        Log.e("AppModule", "Creating FinishCtrlManager...")
        FinishCtrlManager(get(), get())
    }

    single {
        Log.e("AppModule", "Creating SignatureManager...")
        SignatureManager(get(), get())
    }
}

val preferencesModule = module {
    single {
        get<Context>().getSharedPreferences(
            BaseActivity.PREF_SAVED_ID_NAME,
            Context.MODE_PRIVATE
        )
    }
}

val appModule = listOf(
    preferencesModule,
    managerModule,
    viewModelModule
)
