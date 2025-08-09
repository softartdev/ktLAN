package com.softartdev.ktlan.data.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow

/** Provides a list of available network interfaces. */
expect class NetworkInterfacesProvider(dispatchers: CoroutineDispatchers) {
    suspend fun list(): List<NetworkInterfaceInfo>
    fun watch(): Flow<List<NetworkInterfaceInfo>>
}
