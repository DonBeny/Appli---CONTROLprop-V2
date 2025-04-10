package org.orgaprop.controlprop.ui.login.states

sealed class UiState {

    data class ThemeChanged(val isDarkMode: Boolean) : UiState()
    data class LayoutChanged(val newLayout: Int, val oldLayout: Int) : UiState()

}
