@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.socket.SocketEndpoint
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.data.socket.getLocalIp
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.model.ChatMessage.Sender
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository managing one active TCP chat session.
 */
open class SocketRepo(
    private val transport: SocketTransport,
    private val dispatchers: CoroutineDispatchers
) {
    private val scope = CoroutineScope(dispatchers.default)
    private val messages = MutableSharedFlow<ChatMessage>()

    private var stopHandle: (suspend () -> Unit)? = null
    private var connection: SocketTransport.ChatConnection? = null

    /** Stream of incoming chat messages. */
    open fun observeMessages(): Flow<ChatMessage> = messages.asSharedFlow()

    /** Start server and wait for the first incoming client. */
    open suspend fun startServer(bindHost: String, bindPort: Int) {
        Napier.d("Starting server on $bindHost:$bindPort")
        stop()
        val (conn, stop) = transport.startServer(bindHost, bindPort)
        connection = conn
        stopHandle = stop
        collectIncoming(conn)
    }

    /** Connect to remote endpoint. */
    open suspend fun connectTo(remoteHost: String, remotePort: Int) {
        Napier.d("Connecting to $remoteHost:$remotePort")
        stop()
        val conn = transport.connect(SocketEndpoint(remoteHost, remotePort))
        connection = conn
        stopHandle = null
        collectIncoming(conn)
    }

    /** Send a text line to peer. */
    open suspend fun send(text: String) {
        connection?.send(text)
        messages.emit(ChatMessage(Sender.Local, text, now()))
    }

    /** Stop any running server or connection. */
    open suspend fun stop() {
        stopHandle?.invoke()
        stopHandle = null
        connection?.close()
        connection = null
    }

    /** Best effort local IP detection. */
    open suspend fun getLocalIp(): String = getLocalIp(dispatchers)

    private fun collectIncoming(conn: SocketTransport.ChatConnection) {
        scope.launch {
            conn.incoming.collect { line ->
                messages.emit(ChatMessage(Sender.Remote, line, now()))
            }
        }
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
