package com.softartdev.ktlan.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.outlined.ConnectWithoutContact
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import ktlan.app.shared.generated.resources.Res
import ktlan.app.shared.generated.resources.network_interfaces
import ktlan.app.shared.generated.resources.scan
import ktlan.app.shared.generated.resources.settings
import ktlan.app.shared.generated.resources.socket
import ktlan.app.shared.generated.resources.webrtc
import org.jetbrains.compose.resources.StringResource

enum class MainBottomTab(
    val route: AppNavGraph.BottomTab,
    val titleRes: StringResource,
    val iconVector: ImageVector,
    val testTag: String
) {
    Connect(
        route = AppNavGraph.BottomTab.Connect,
        titleRes = Res.string.webrtc,
        iconVector = Icons.Outlined.ConnectWithoutContact,
        testTag = "webrtc_tab"
    ),
    Scan(
        route = AppNavGraph.BottomTab.Scan(),
        titleRes = Res.string.scan,
        iconVector = Icons.Default.Radar,
        testTag = "scan_tab"
    ),
    Networks(
        route = AppNavGraph.BottomTab.Networks,
        titleRes = Res.string.network_interfaces,
        iconVector = Icons.Default.SettingsEthernet,
        testTag = "network_interfaces_tab"
    ),
    Socket(
        route = AppNavGraph.BottomTab.Socket(),
        titleRes = Res.string.socket,
        iconVector = Icons.Default.Cable,
        testTag = "socket_tab"
    ),
    Settings(
        route = AppNavGraph.BottomTab.Settings,
        titleRes = Res.string.settings,
        iconVector = Icons.Default.Settings,
        testTag = "settings_tab"
    );

    fun isSelected(currentBackStackEntry: NavBackStackEntry?): Boolean {
        val routeName: String = this.route::class.simpleName.orEmpty()
        return currentBackStackEntry?.destination?.route?.contains(routeName) ?: false
    }
}