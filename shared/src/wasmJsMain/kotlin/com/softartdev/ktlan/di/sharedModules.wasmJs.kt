package com.softartdev.ktlan.di

import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.data.webrtc.ServerlessRTCClient
import com.softartdev.ktlan.data.webrtc.WasmJsClientWebRTC
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val dataModule: Module = module {
    factoryOf(::WasmJsClientWebRTC) bind ServerlessRTCClient::class
    singleOf(::SocketTransport)
}