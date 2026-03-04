@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.main

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.softartdev.ktlan.connect.ConnectContent
import com.softartdev.ktlan.connect.ConnectScreen
import com.softartdev.ktlan.connect.PreviewCameraPermissionState
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiModules
import com.softartdev.ktlan.isImeVisible
import com.softartdev.ktlan.networks.NetworksContent
import com.softartdev.ktlan.networks.NetworksScreen
import com.softartdev.ktlan.presentation.connect.ConnectResult
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import com.softartdev.ktlan.presentation.networks.NetworksResult
import com.softartdev.ktlan.presentation.scan.ScanResult
import com.softartdev.ktlan.presentation.scan.ScanViewModel
import com.softartdev.ktlan.presentation.socket.SocketResult
import com.softartdev.ktlan.presentation.socket.SocketViewModel
import com.softartdev.ktlan.scan.ScanContent
import com.softartdev.ktlan.scan.ScanScreen
import com.softartdev.ktlan.settings.SettingsContent
import com.softartdev.ktlan.settings.SettingsScreen
import com.softartdev.ktlan.socket.SocketConnectContent
import com.softartdev.ktlan.socket.SocketConnectScreen
import com.softartdev.theme.material3.PreferableMaterialTheme
import ktlan.app.shared.generated.resources.Res
import ktlan.app.shared.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MainBottomNavScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startBottomTab: AppNavGraph.BottomTab = AppNavGraph.BottomTab.Connect,
) {
    val navBackStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
    val router: Router = koinInject()
    DisposableEffect(navController, router) {
        router.setBottomNavController(navController)
        onDispose(router::releaseBottomNavController)
    }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(Res.string.app_name)) }) },
        content = { paddingValues: PaddingValues ->
            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = navController,
                startDestination = startBottomTab,
                builder = when (LocalInspectionMode.current) { // Check if we are in preview mode
                    true -> previewNavGraphBuilder()
                    else -> mainNavGraphBuilder()
                },
            )
        },
        bottomBar = {
            if (!WindowInsets.isImeVisible) NavigationBar {
                MainBottomTab.entries.forEach { bottomTab: MainBottomTab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(bottomTab.testTag),
                        selected = bottomTab.isSelected(navBackStackEntry),
                        onClick = { router.bottomNavigate(bottomTab.route) },
                        label = {
                            Text(
                                modifier = Modifier.basicMarquee(),
                                text = stringResource(resource = bottomTab.titleRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = bottomTab.iconVector,
                                contentDescription = null
                            )
                        },
                    )
                }
            }
        },
    )
}

private fun mainNavGraphBuilder(): NavGraphBuilder.() -> Unit = {
    composable<AppNavGraph.BottomTab.Connect> {
        ConnectScreen(connectViewModel = koinViewModel())
    }
    composable<AppNavGraph.BottomTab.Scan> { backStackEntry: NavBackStackEntry ->
        val route: AppNavGraph.BottomTab.Scan = backStackEntry.toRoute()
        ScanScreen(scanViewModel = koinViewModel<ScanViewModel> { parametersOf(route) })
    }
    composable<AppNavGraph.BottomTab.Networks> {
        NetworksScreen(viewModel = koinViewModel())
    }
    composable<AppNavGraph.BottomTab.Socket> { backStackEntry: NavBackStackEntry ->
        val route: AppNavGraph.BottomTab.Socket = backStackEntry.toRoute()
        SocketConnectScreen(viewModel = koinViewModel<SocketViewModel> { parametersOf(route) })
    }
    composable<AppNavGraph.BottomTab.Settings> {
        SettingsScreen(settingsViewModel = koinViewModel())
    }
}

@Preview
@Composable
fun MainBottomNavScreenPreview() = KoinApplicationPreview(
    application = { modules(sharedModules + uiModules) }
) {
    PreferableMaterialTheme { MainBottomNavScreen() }
}

private fun previewNavGraphBuilder(): NavGraphBuilder.() -> Unit = {
    composable<AppNavGraph.BottomTab.Connect> {
        ConnectContent(
            result = ConnectResult(consoleMessages = ConnectResult.previewMessages),
            onAction = {},
            cameraPermissionState = PreviewCameraPermissionState()
        )
    }
    composable<AppNavGraph.BottomTab.Scan> {
        ScanContent(
            onAction = {},
            scanResult = ScanResult.Success(hosts = ScanResult.Success.previewHosts)
        )
    }
    composable<AppNavGraph.BottomTab.Networks> {
        NetworksContent(
            result = NetworksResult(interfaces = NetworksResult.previewInterfaces),
            onAction = {}
        )
    }
    composable<AppNavGraph.BottomTab.Socket> {
        SocketConnectContent(
            result = SocketResult(
                connected = true,
                messages = SocketResult.previewMessages,
                bindHost = "192.168.1.2"
            ),
            onAction = {}
        )
    }
    composable<AppNavGraph.BottomTab.Settings> {
        SettingsContent(onAction = {})
    }
}
