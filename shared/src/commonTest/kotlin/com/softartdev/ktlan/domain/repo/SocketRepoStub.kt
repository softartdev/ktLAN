package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableSharedFlow

class SocketRepoStub(dispatchers: CoroutineDispatchers) : SocketRepo(SocketTransport(dispatchers), dispatchers) {
    val sent = mutableListOf<String>()
    private val _messages = MutableSharedFlow<ChatMessage>()
    override fun observeMessages() = _messages
    override suspend fun getLocalIp(): String = "192.168.0.1"
    override suspend fun startServer(bindHost: String, bindPort: Int) {}
    override suspend fun connectTo(remoteHost: String, remotePort: Int) {}
    override suspend fun send(text: String) { sent += text }
    override suspend fun stop() {}
}

