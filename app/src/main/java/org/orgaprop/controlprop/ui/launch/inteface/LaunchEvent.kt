package org.orgaprop.controlprop.ui.launch.inteface

sealed class LaunchEvent {

    object NavigateToMain : LaunchEvent()
    data class ShowError(val message: String) : LaunchEvent()

}
