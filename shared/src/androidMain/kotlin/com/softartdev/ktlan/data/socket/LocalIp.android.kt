package com.softartdev.ktlan.data.socket

import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

/** Android implementation retrieving the first non-loopback IPv4 address. */
actual suspend fun getLocalIp(dispatchers: CoroutineDispatchers): String = withContext(dispatchers.io) {
    return@withContext try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "0.0.0.0"
    } catch (_: Throwable) {
        "0.0.0.0"
    }
}
