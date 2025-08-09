@file:OptIn(ExperimentalForeignApi::class)

package com.softartdev.ktlan.data.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.IFF_LOOPBACK
import platform.posix.IFF_MULTICAST
import platform.posix.IFF_UP
import platform.posix.INET6_ADDRSTRLEN
import platform.posix.INET_ADDRSTRLEN
import platform.posix.sockaddr_in
import platform.posix.sockaddr_in6

actual class NetworkInterfacesProvider actual constructor(private val dispatchers: CoroutineDispatchers) {
    actual suspend fun list(): List<NetworkInterfaceInfo> = withContext(dispatchers.io) {
        memScoped {
            val ifap = alloc<CPointerVar<ifaddrs>>()
            val result = mutableMapOf<String, MutableInterfaceData>()
            if (getifaddrs(ifap.ptr) == 0) {
                var ptr: CPointer<ifaddrs>? = ifap.value
                while (ptr != null) {
                    val ifa = ptr.pointed
                    val name = ifa.ifa_name?.toKString() ?: ""
                    val flags = ifa.ifa_flags.toInt()
                    val data = result.getOrPut(name) {
                        MutableInterfaceData(
                            isUp = flags and IFF_UP != 0,
                            isLoopback = flags and IFF_LOOPBACK != 0,
                            supportsMulticast = flags and IFF_MULTICAST != 0
                        )
                    }
                    val addr = ifa.ifa_addr
                    if (addr != null) {
                        when (addr.pointed.sa_family.toInt()) {
                            AF_INET -> {
                                val buf = allocArray<ByteVar>(INET_ADDRSTRLEN)
                                val sa: CPointer<sockaddr_in> = addr.reinterpret<sockaddr_in>()
                                inet_ntop(AF_INET, sa.pointed.sin_addr.ptr, buf, INET_ADDRSTRLEN.toUInt())
                                data.ipv4 += buf.toKString()
                            }
                            AF_INET6 -> {
                                val buf = allocArray<ByteVar>(INET6_ADDRSTRLEN)
                                val sa = addr.reinterpret<sockaddr_in6>()
                                inet_ntop(AF_INET6, sa.pointed.sin6_addr.ptr, buf, INET6_ADDRSTRLEN.toUInt())
                                data.ipv6 += buf.toKString()
                            }
                        }
                    }
                    ptr = ifa.ifa_next
                }
                freeifaddrs(ifap.value)
            }
            result.map { (name, d) ->
                NetworkInterfaceInfo(
                    name = name,
                    isUp = d.isUp,
                    isLoopback = d.isLoopback,
                    supportsMulticast = d.supportsMulticast,
                    ipv4 = d.ipv4,
                    ipv6 = d.ipv6
                )
            }
        }
    }

    actual fun watch(): Flow<List<NetworkInterfaceInfo>> = flow { emit(list()) }.flowOn(dispatchers.io)
}

private data class MutableInterfaceData(
    val isUp: Boolean,
    val isLoopback: Boolean,
    val supportsMulticast: Boolean,
    val ipv4: MutableList<String> = mutableListOf(),
    val ipv6: MutableList<String> = mutableListOf()
)
