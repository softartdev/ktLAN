package com.softartdev.ktlan

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*

class ApplicationApi {
    private val client: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) = Napier.d(tag = "Ktor", message = message)
            }
        }
        followRedirects = true
    }
    private val address = Url("http://checkip.amazonaws.com/")

    suspend fun publicIp(): String = client.get { url(address) }.bodyAsText()

    suspend fun localIp(): String? {
        val selector = SelectorManager(dispatcher = Dispatchers.Default)
        Napier.d(selector.toString())
        val socket = aSocket(selector).tcp().connect(hostname = "8.8.8.8", port = 53)
        Napier.d(socket.toString())
        val inetSocketAddress = socket.localAddress as? InetSocketAddress ?: return null
        Napier.d(inetSocketAddress.toString())
        return inetSocketAddress.hostname
    }
}
