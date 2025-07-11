@file:OptIn(KoinExperimentalAPI::class)

package com.softartdev.ktlan

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiModules
import com.softartdev.ktlan.main.MainBottomNavScreen
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import com.softartdev.ktlan.qr.QrDialogContent
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.ThemeDialogContent
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration

@Composable
fun App() = KoinMultiplatformApplication(
    config = koinConfiguration { modules(sharedModules + uiModules) },
    logLevel = Level.DEBUG
) {
    LaunchedEffect(Unit) { AppState.launch() }
    PreferableMaterialTheme {
        val navController = rememberNavController()
        val router: Router = koinInject()
        DisposableEffect(key1 = navController, key2 = router) {
            router.setController(navController)
            onDispose(router::releaseController)
        }
        EnableEdgeToEdge()
        NavHost(
            modifier = Modifier.imePadding(),
            navController = navController,
            startDestination = AppNavGraph.MainBottomNav,
        ) {
            composable<AppNavGraph.MainBottomNav> {
                MainBottomNavScreen()
            }
            dialog<AppNavGraph.QrDialog> { backStackEntry: NavBackStackEntry ->
                QrDialogContent(
                    text = backStackEntry.toRoute<AppNavGraph.QrDialog>().text,
                    dismissDialog = navController::navigateUp
                )
            }
            dialog<AppNavGraph.ThemeDialog> {
                ThemeDialogContent(dismissDialog = navController::popBackStack)
            }
            dialog<AppNavGraph.ErrorDialog> { backStackEntry: NavBackStackEntry ->
                Error(
                    message = backStackEntry.toRoute<AppNavGraph.ErrorDialog>().message.orEmpty(),
                    onRetry = navController::navigateUp,
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
