package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.socket.SocketEndpoint
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.model.ChatMessage.Sender
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Repository managing a single TCP chat session.
 */
class SocketRepo(
    private val dispatchers: CoroutineDispatchers,
) {
    private val scope = CoroutineScope(dispatchers.io)
    private var connectionJob: Job? = null
    private var connection: SocketTransport.ChatConnection? = null
    private var stopServer: (suspend () -> Unit)? = null

    private val mutableMessages = MutableSharedFlow<ChatMessage>()
    val messages: Flow<ChatMessage> = mutableMessages.asSharedFlow()

    /** Starts a server and waits for the first client. */
    suspend fun startServer(bindHost: String, bindPort: Int) {
        stop()
        val (conn, stop) = SocketTransport.startServer(bindHost, bindPort)
        connection = conn
        stopServer = stop
        observeIncoming(conn)
    }

    /** Connects to a remote host. */
    suspend fun connectTo(remoteHost: String, remotePort: Int) {
        stop()
        val conn = SocketTransport.connect(SocketEndpoint(remoteHost, remotePort))
        connection = conn
        observeIncoming(conn)
    }

    /** Sends a text line to the peer. */
    suspend fun send(text: String) {
        val conn = connection ?: return
        conn.send(text)
        mutableMessages.emit(
            ChatMessage(Sender.Local, text, Clock.System.now().toEpochMilliseconds())
        )
    }

    /** Stops any running server and closes current connection. */
    suspend fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        connection?.close()
        connection = null
        stopServer?.invoke()
        stopServer = null
    }

    private fun observeIncoming(conn: SocketTransport.ChatConnection) {
        connectionJob = scope.launch {
            conn.incoming.collect { line ->
                mutableMessages.emit(
                    ChatMessage(
                        Sender.Remote,
                        line,
                        Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }
}
