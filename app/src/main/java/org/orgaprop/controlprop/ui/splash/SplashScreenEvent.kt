package org.orgaprop.controlprop.ui.splash

sealed class SplashScreenEvent {

    object NavigateToMain : SplashScreenEvent()
    data class ShowError(val message: String) : SplashScreenEvent()

}
