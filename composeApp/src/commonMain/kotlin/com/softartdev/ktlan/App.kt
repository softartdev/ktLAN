@file:OptIn(KoinExperimentalAPI::class)

package com.softartdev.ktlan

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.softartdev.ktlan.main.MainScreen
import com.softartdev.theme.material3.PreferableMaterialTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@Composable
fun App() = KoinMultiplatformApplication(
    config = koinConfiguration { modules(sharedModules) }
) {
    PreferableMaterialTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(mainViewModel = koinViewModel())
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
