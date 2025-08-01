package com.softartdev.ktlan.data.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

actual class NetworkInterfacesProvider actual constructor(private val dispatchers: CoroutineDispatchers) {
    actual suspend fun list(): List<NetworkInterfaceInfo> = withContext(dispatchers.io) {
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .sortedBy { it.index }
                .map { ni ->
                    val ipv4 = ni.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .map { it.hostAddress }
                    val ipv6 = ni.inetAddresses.toList()
                        .filterIsInstance<Inet6Address>()
                        .map { it.hostAddress }
                    NetworkInterfaceInfo(
                        name = ni.name,
                        isUp = ni.isUp,
                        isLoopback = ni.isLoopback,
                        supportsMulticast = ni.supportsMulticast(),
                        ipv4 = ipv4,
                        ipv6 = ipv6,
                        mtu = runCatching { ni.mtu }.getOrNull(),
                        index = runCatching { ni.index }.getOrNull()
                    )
                }
        }.getOrElse { emptyList() }
    }

    actual fun watch(): Flow<List<NetworkInterfaceInfo>> = flow { emit(list()) }.flowOn(dispatchers.io)
}
