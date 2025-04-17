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
import org.orgaprop.controlprop.utils.LogUtils
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
        LogUtils.e("AppModule", "Creating MainViewModel...")
        MainViewModel()
    }

    viewModel {
        LogUtils.e("AppModule", "Creating LoginViewModel...")
        LoginViewModel(get(), get(), get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating GetMailViewModel...")
        GetMailViewModel(get(), get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating SelectEntryViewModel...")
        SelectEntryViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating SelectListViewModel...")
        SelectListViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating TypeCtrlViewModel...")
        TypeCtrlViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating PlanActionsViewModel...")
        PlanActionsViewModel(get(), get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating ConfigCtrlViewModel...")
        ConfigCtrlViewModel()
    }

    viewModel {
        LogUtils.e("AppModule", "Creating GrilleCtrlViewModel...")
        GrilleCtrlViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating SendMailViewModel...")
        SendMailViewModel(get(), get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating CtrlZoneViewModel...")
        CtrlZoneViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating AddCommentViewModel...")
        AddCommentViewModel()
    }

    viewModel {
        LogUtils.e("AppModule", "Creating FinishCtrlViewModel...")
        FinishCtrlViewModel(get())
    }

    viewModel {
        LogUtils.e("AppModule", "Creating SignatureViewModel...")
        SignatureViewModel(get())
    }
}

val managerModule = module {
    single {
        LogUtils.e("AppModule", "Creating HttpTask...")
        HttpTask(get())
    }

    single {
        LogUtils.e("AppModule", "Creating LoginManager...")
        LoginManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating LoginRepository...")
        LoginRepository(get())
    }

    single {
        LogUtils.e("AppModule", "Creating LoginResponseMapper...")
        LoginResponseMapper()
    }

    single {
        LogUtils.e("AppModule", "Creating SecureCredentialsManager...")
        SecureCredentialsManager(get())
    }

    single {
        LogUtils.e("AppModule", "Starting network monitoring...")
        val context = get<Context>()
        NetworkMonitor.startMonitoring(context)
        NetworkMonitor
    }

    single {
        LogUtils.e("AppModule", "Creating GetMailManager...")
        GetMailManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating SelectEntryManager...")
        SelectEntryManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating SelectListManager...")
        SelectListManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating TypeCtrlManager...")
        TypeCtrlManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating PlanActionsManager...")
        PlanActionsManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating GrilleCtrlManager...")
        GrilleCtrlManager(get(), get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating CtrlZoneManager...")
        CtrlZoneManager(get())
    }

    single {
        LogUtils.e("AppModule", "Creating SendMailManager...")
        SendMailManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating SyncManager...")
        SyncManager(get(), get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating FinishCtrlManager...")
        FinishCtrlManager(get(), get())
    }

    single {
        LogUtils.e("AppModule", "Creating SignatureManager...")
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
