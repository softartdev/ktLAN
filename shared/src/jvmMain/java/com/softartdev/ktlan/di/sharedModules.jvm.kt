package com.softartdev.ktlan.di

import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.data.networks.NetworkInterfacesProvider
import com.softartdev.ktlan.data.webrtc.JvmClientWebRTC
import com.softartdev.ktlan.data.webrtc.ServerlessRTCClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val dataModule: org.koin.core.module.Module = module {
    factoryOf(::JvmClientWebRTC) bind ServerlessRTCClient::class
    singleOf(::SocketTransport)
    singleOf(::NetworkInterfacesProvider)
}