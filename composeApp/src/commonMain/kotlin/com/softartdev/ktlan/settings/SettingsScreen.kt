package com.softartdev.ktlan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.softartdev.ktlan.Error
import com.softartdev.ktlan.Loader
import com.softartdev.ktlan.presentation.settings.SettingsAction
import com.softartdev.ktlan.presentation.settings.SettingsResult
import com.softartdev.ktlan.presentation.settings.SettingsViewModel
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.ThemePreferenceItem
import com.softartdev.theme.material3.ThemePreferencesCategory
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val result: SettingsResult by settingsViewModel.stateFlow.collectAsState()
    SettingsScreen(result, settingsViewModel::onAction)
}

@Composable
fun SettingsScreen(result: SettingsResult, onAction: (SettingsAction) -> Unit) {
    when (result) {
        is SettingsResult.Loading -> Loader()
        is SettingsResult.Success -> SettingsContent(onAction = onAction)
        is SettingsResult.Error -> Error(message = result.message)
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onAction: (SettingsAction) -> Unit
) {
    Column(modifier = modifier) {
        ThemePreferencesCategory()
        ThemePreferenceItem(onClick = { onAction(SettingsAction.ShowThemeDialog) })
    }
}

@Preview
@Composable
fun SettingsScreenPreview() = PreferableMaterialTheme {
    SettingsContent(onAction = {})
}
