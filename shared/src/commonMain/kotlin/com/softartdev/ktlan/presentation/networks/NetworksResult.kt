package com.softartdev.ktlan.presentation.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo

/** UI state for Networks screen. */
data class NetworksResult(
    val loading: Boolean = false,
    val interfaces: List<NetworkInterfaceInfo> = emptyList(),
    val error: String? = null,
    val selectedIp: String? = null,
    val showAll: Boolean = false,
)

sealed interface NetworksAction {
    data object Refresh : NetworksAction
    data class ToggleShowAll(val show: Boolean) : NetworksAction
    data class Copy(val address: String) : NetworksAction
    data class UseAsBindHost(val address: String) : NetworksAction
}
