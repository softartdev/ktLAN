package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.socket.SocketEndpoint
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.model.ChatMessage.Sender
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Repository managing a single TCP chat session.
 */
class SocketRepo(
    private val coroutineDispatchers: CoroutineDispatchers
) {
    private val scope = CoroutineScope(coroutineDispatchers.default)

    private var serverStop: (suspend () -> Unit)? = null
    private var connection: SocketTransport.ChatConnection? = null

    private val messagesFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 8)
    val messages: Flow<ChatMessage> = messagesFlow

    /** Starts server and waits for first connection. */
    suspend fun startServer(bindHost: String, bindPort: Int) {
        stop()
        val (stopHandle, conn) = SocketTransport.startServer(bindHost, bindPort)
        serverStop = stopHandle
        connection = conn
        scope.launch { collectIncoming(conn) }
    }

    /** Connects to remote host. */
    suspend fun connectTo(remoteHost: String, remotePort: Int) {
        stop()
        val conn = SocketTransport.connect(SocketEndpoint(remoteHost, remotePort))
        connection = conn
        scope.launch { collectIncoming(conn) }
    }

    /** Sends a chat message to the peer. */
    suspend fun send(text: String) {
        connection?.send(text)
        messagesFlow.emit(ChatMessage(Sender.Local, text, Clock.System.now().toEpochMilliseconds()))
    }

    /** Stops server and closes the active connection. */
    suspend fun stop() {
        connection?.close()
        connection = null
        serverStop?.invoke()
        serverStop = null
    }

    private suspend fun collectIncoming(conn: SocketTransport.ChatConnection) {
        conn.incoming.collect { line ->
            messagesFlow.emit(ChatMessage(Sender.Remote, line, Clock.System.now().toEpochMilliseconds()))
        }
    }
}
