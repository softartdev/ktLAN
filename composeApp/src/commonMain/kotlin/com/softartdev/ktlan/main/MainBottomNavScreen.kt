@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Plagiarism
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.softartdev.ktlan.connect.ConnectScreen
import com.softartdev.ktlan.isImeVisible
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.scan.ScanScreen
import com.softartdev.ktlan.settings.SettingsScreen
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.app_name
import ktlan.composeapp.generated.resources.connection
import ktlan.composeapp.generated.resources.scan
import ktlan.composeapp.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainBottomNavScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startBottomTab: AppNavGraph.BottomTab = AppNavGraph.BottomTab.Connect
) {
    var selectedTab: AppNavGraph.BottomTab by remember { mutableStateOf(startBottomTab) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(Res.string.app_name)) }) },
        content = { paddingValues: PaddingValues ->
            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = navController,
                startDestination = startBottomTab,
            ) {
                composable<AppNavGraph.BottomTab.Scan> {
                    selectedTab = AppNavGraph.BottomTab.Scan
                    ScanScreen(scanViewModel = koinViewModel())
                }
                composable<AppNavGraph.BottomTab.Connect> {
                    selectedTab = AppNavGraph.BottomTab.Connect
                    ConnectScreen(connectViewModel = koinViewModel())
                }
                composable<AppNavGraph.BottomTab.Settings> {
                    selectedTab = AppNavGraph.BottomTab.Settings
                    SettingsScreen(settingsViewModel = koinViewModel())
                }
            }
        },
        bottomBar = {
            if (!WindowInsets.isImeVisible) NavigationBar {
                sequenceOf(
                    AppNavGraph.BottomTab.Scan,
                    AppNavGraph.BottomTab.Connect,
                    AppNavGraph.BottomTab.Settings
                ).forEach { bottomTab: AppNavGraph.BottomTab ->
                    NavigationBarItem(
                        selected = selectedTab == bottomTab,
                        onClick = {
                            navController.navigate(bottomTab) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    saveState = true
                                }
                                launchSingleTop = true // Avoid multiple copies of the same destination when reselecting the same item
                                restoreState = true // Restore state when reselecting a previously selected item
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(
                                    resource = when (bottomTab) {
                                        AppNavGraph.BottomTab.Connect -> Res.string.connection
                                        AppNavGraph.BottomTab.Scan -> Res.string.scan
                                        AppNavGraph.BottomTab.Settings -> Res.string.settings
                                    }
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (bottomTab) {
                                    AppNavGraph.BottomTab.Scan -> Icons.Default.Plagiarism
                                    AppNavGraph.BottomTab.Connect -> Icons.Outlined.ConnectWithoutContact
                                    AppNavGraph.BottomTab.Settings -> Icons.Default.Settings
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

@Preview
@Composable
fun MainBottomNavScreenPreview() {
    MainBottomNavScreen()
}