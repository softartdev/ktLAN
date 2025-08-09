package com.softartdev.ktlan.data.networks

import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

actual class NetworkInterfacesProvider actual constructor(private val dispatchers: CoroutineDispatchers) {
    actual suspend fun list(): List<NetworkInterfaceInfo> = emptyList()
    actual fun watch(): Flow<List<NetworkInterfaceInfo>> = flow<List<NetworkInterfaceInfo>> { emit(emptyList()) }.flowOn(dispatchers.io)
}
