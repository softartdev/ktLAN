package com.softartdev.ktlan.domain.model

/** Information about a network interface. */
data class NetworkInterfaceInfo(
    val name: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val supportsMulticast: Boolean,
    val ipv4: List<String> = emptyList(),
    val ipv6: List<String> = emptyList(),
    val mtu: Int? = null,
    val index: Int? = null,
)
