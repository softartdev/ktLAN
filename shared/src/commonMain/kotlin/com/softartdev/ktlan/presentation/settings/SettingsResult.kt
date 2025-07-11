package com.softartdev.ktlan.presentation.settings

sealed interface SettingsResult {
    data object Loading : SettingsResult
    data class Success(val message: String = "Saved") : SettingsResult
    data class Error(val message: String) : SettingsResult
}

sealed interface SettingsAction {

    data object ShowThemeDialog : SettingsAction
}