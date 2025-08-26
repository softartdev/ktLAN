package com.softartdev.ktlan.presentation.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo

data class NetworksResult(
    val loading: Boolean = false,
    val yourIp: String? = null,
    val interfaces: List<NetworkInterfaceInfo> = emptyList(),
    val error: String? = null,
) {
    companion object {
        val previewInterfaces: List<NetworkInterfaceInfo>
            get() = listOf(
                NetworkInterfaceInfo(
                    name = "eth0",
                    isUp = true,
                    isLoopback = false,
                    supportsMulticast = true,
                    ipv4 = listOf("192.168.0.10"),
                    ipv6 = listOf("fe80::1")
                ),
                NetworkInterfaceInfo(
                    name = "lo",
                    isUp = true,
                    isLoopback = true,
                    supportsMulticast = false,
                    ipv4 = listOf("127.0.0.1"),
                    ipv6 = emptyList()
                )
            )
    }
}

sealed interface NetworksAction {
    data object Refresh : NetworksAction
    data class UseAsBindHost(val address: String) : NetworksAction
    data class Scan(val address: String) : NetworksAction
}
