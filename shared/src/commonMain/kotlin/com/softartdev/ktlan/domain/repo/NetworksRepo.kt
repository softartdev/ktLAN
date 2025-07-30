package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.networks.NetworkInterfacesProvider
import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.withContext

/** Repository providing network interface information. */
class NetworksRepo(
    private val provider: NetworkInterfacesProvider,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun listInterfaces(): List<NetworkInterfaceInfo> = provider.list()

    suspend fun guessLocalIPv4(): String? {
        val interfaces = provider.list()
        val filtered = interfaces.filter { it.isUp && !it.isLoopback }
        val addresses = filtered.flatMap { it.ipv4 }
        val preferred = addresses.firstOrNull { it.isPrivateIpv4() }
        return preferred ?: addresses.firstOrNull()
    }
}

private fun String.isPrivateIpv4(): Boolean {
    if (startsWith("10.")) return true
    if (startsWith("192.168.")) return true
    if (startsWith("172.")) {
        val part = split(".").getOrNull(1)?.toIntOrNull()
        if (part != null && part in 16..31) return true
    }
    return false
}
