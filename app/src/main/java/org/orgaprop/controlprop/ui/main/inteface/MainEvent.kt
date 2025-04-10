package org.orgaprop.controlprop.ui.main.inteface

sealed class MainEvent {

    data object NavigateToMain : MainEvent()
    data class ShowError(val message: String) : MainEvent()

}
