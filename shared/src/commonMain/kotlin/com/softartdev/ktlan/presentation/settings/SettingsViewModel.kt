package com.softartdev.ktlan.presentation.settings

import androidx.lifecycle.ViewModel
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val router: Router) : ViewModel() {
    private val mutableStateFlow: MutableStateFlow<SettingsResult> = MutableStateFlow(
        value = SettingsResult.Success()
    )
    val stateFlow: StateFlow<SettingsResult> = mutableStateFlow

    private val currentData: SettingsResult.Success
        get() = mutableStateFlow.value as? SettingsResult.Success ?: SettingsResult.Success()

    fun onAction(action: SettingsAction) = when (action) {
        SettingsAction.ShowThemeDialog -> router.navigate(AppNavGraph.ThemeDialog)
    }
}

