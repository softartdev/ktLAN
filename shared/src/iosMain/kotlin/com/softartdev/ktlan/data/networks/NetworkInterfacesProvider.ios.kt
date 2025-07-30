package com.softartdev.ktlan.data.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.IFF_LOOPBACK
import platform.posix.IFF_MULTICAST
import platform.posix.IFF_UP
import platform.posix.INET6_ADDRSTRLEN
import platform.posix.INET_ADDRSTRLEN
import platform.posix.getifaddrs
import platform.posix.ifaddrs
import platform.posix.inet_ntop
import platform.posix.freeifaddrs
import platform.posix.sockaddr_in
import platform.posix.sockaddr_in6

actual class NetworkInterfacesProvider actual constructor(private val dispatchers: CoroutineDispatchers) {
    actual suspend fun list(): List<NetworkInterfaceInfo> = withContext(dispatchers.io) {
        memScoped {
            val ifap = alloc<CPointerVar<ifaddrs>>()
            val result = mutableMapOf<String, MutableInterfaceData>()
            if (getifaddrs(ifap.ptr) == 0) {
                var ptr = ifap.value
                while (ptr != null) {
                    val ifa = ptr.pointed
                    val name = ifa.ifa_name?.toKString() ?: ""
                    val flags = ifa.ifa_flags.toInt()
                    val data = result.getOrPut(name) {
                        MutableInterfaceData(
                            isUp = flags and IFF_UP.toInt() != 0,
                            isLoopback = flags and IFF_LOOPBACK.toInt() != 0,
                            supportsMulticast = flags and IFF_MULTICAST.toInt() != 0
                        )
                    }
                    val addr = ifa.ifa_addr
                    if (addr != null) {
                        when (addr.pointed.sa_family.toInt()) {
                            AF_INET -> {
                                val buf = allocArray<ByteVar>(INET_ADDRSTRLEN)
                                val sa = addr.reinterpret<sockaddr_in>()
                                inet_ntop(AF_INET, sa.ptr.pointed.sin_addr.ptr, buf, INET_ADDRSTRLEN.toULong())
                                data.ipv4 += buf.toKString()
                            }
                            AF_INET6 -> {
                                val buf = allocArray<ByteVar>(INET6_ADDRSTRLEN)
                                val sa = addr.reinterpret<sockaddr_in6>()
                                inet_ntop(AF_INET6, sa.ptr.pointed.sin6_addr.ptr, buf, INET6_ADDRSTRLEN.toULong())
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
