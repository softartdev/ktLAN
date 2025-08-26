@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Plagiarism
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ConnectWithoutContact
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
import com.softartdev.ktlan.presentation.socket.SocketResult
import com.softartdev.ktlan.presentation.socket.SocketViewModel
import com.softartdev.ktlan.scan.ScanContent
import com.softartdev.ktlan.scan.ScanScreen
import com.softartdev.ktlan.settings.SettingsContent
import com.softartdev.ktlan.settings.SettingsScreen
import com.softartdev.ktlan.socket.SocketConnectContent
import com.softartdev.ktlan.socket.SocketConnectScreen
import com.softartdev.theme.material3.PreferableMaterialTheme
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.app_name
import ktlan.composeapp.generated.resources.connection
import ktlan.composeapp.generated.resources.lan_chat
import ktlan.composeapp.generated.resources.networks_title
import ktlan.composeapp.generated.resources.scan
import ktlan.composeapp.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.softartdev.ktlan.presentation.scan.ScanViewModel

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
                sequenceOf(
                    AppNavGraph.BottomTab.Connect,
                    AppNavGraph.BottomTab.Scan(),
                    AppNavGraph.BottomTab.Networks,
                    AppNavGraph.BottomTab.Socket(),
                    AppNavGraph.BottomTab.Settings
                ).forEach { bottomTab: AppNavGraph.BottomTab ->
                    NavigationBarItem(
                        selected = navBackStackEntry?.destination?.route
                            ?.contains(bottomTab::class.simpleName.orEmpty()) ?: false,
                        onClick = { router.bottomNavigate(bottomTab) },
                        label = {
                            Text(
                                text = stringResource(
                                    resource = when (bottomTab) {
                                        is AppNavGraph.BottomTab.Connect -> Res.string.connection
                                        is AppNavGraph.BottomTab.Scan -> Res.string.scan
                                        is AppNavGraph.BottomTab.Networks -> Res.string.networks_title
                                        is AppNavGraph.BottomTab.Socket -> Res.string.lan_chat
                                        is AppNavGraph.BottomTab.Settings -> Res.string.settings
                                    }
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (bottomTab) {
                                    is AppNavGraph.BottomTab.Connect -> Icons.Outlined.ConnectWithoutContact
                                    is AppNavGraph.BottomTab.Scan -> Icons.Default.Search
                                    is AppNavGraph.BottomTab.Networks -> Icons.Default.Plagiarism
                                    is AppNavGraph.BottomTab.Socket -> Icons.AutoMirrored.Filled.Chat
                                    is AppNavGraph.BottomTab.Settings -> Icons.Default.Settings
                                },
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
