package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.networks.NetworkInterfacesProvider
import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers

class NetworkRepoStub(dispatchers: CoroutineDispatchers) : NetworksRepo(
    provider = NetworkInterfacesProvider(dispatchers),
    dispatchers = dispatchers
) {
    override suspend fun listInterfaces(): List<NetworkInterfaceInfo> = emptyList()
    override suspend fun guessLocalIPv4(): String? = "192.168.0.1"
}