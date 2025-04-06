package org.orgaprop.controlprop.ui.launch.inteface

sealed class LaunchEvent {

    data object NavigateToMain : LaunchEvent()
    data class ShowError(val message: String) : LaunchEvent()

}
