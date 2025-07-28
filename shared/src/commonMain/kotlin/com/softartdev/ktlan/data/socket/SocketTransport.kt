package com.softartdev.ktlan.data.socket

import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import io.github.aakira.napier.Napier
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.collections.set

/**
 * Simple TCP transport using ktor-network and line based protocol.
 */
class SocketTransport(private val dispatchers: CoroutineDispatchers) {
    private val selectorManager = SelectorManager(dispatchers.io)

    /** A chat connection with line based incoming messages. */
    class ChatConnection internal constructor(
        private val socket: Socket,
        private val dispatchers: CoroutineDispatchers
    ) {
        private val reader: ByteReadChannel = socket.openReadChannel()
        private val writer = socket.openWriteChannel(autoFlush = true)

        /** Stream of incoming lines. */
        val incoming: Flow<String> = flow {
            while (true) {
                val line = withContext(dispatchers.io) { reader.readUTF8Line() } ?: break
                emit(line)
            }
        }.flowOn(dispatchers.io)

        /** Send a line to peer. */
        suspend fun send(line: String) = withContext(dispatchers.io) {
            writer.writeStringUtf8(line + "\n")
        }

        /** Close the underlying socket. */
        suspend fun close() = withContext(dispatchers.io) {
            socket.close()
        }
    }

    /** Start server and wait for the first connection. */
    suspend fun startServer(bindHost: String, bindPort: Int): Pair<ChatConnection, suspend () -> Unit> {
        Napier.d("Starting server on $bindHost:$bindPort")
        val server: ServerSocket = aSocket(selectorManager).tcp().bind(bindHost, bindPort)
        Napier.d("Server started at local address: ${server.localAddress}")
        val socket: Socket = server.accept()
        Napier.d("Accepted connection from: ${socket.remoteAddress}")
        val connection = ChatConnection(socket, dispatchers)
        Napier.d("Connection established, ready to receive messages")
        val stop: suspend () -> Unit = {
            Napier.d("Stopping server and closing connection")
            withContext(dispatchers.io) { server.close() }
        }
        Napier.d("Server ready, returning connection and stop function")
        return connection to stop
    }

    /** Connect to remote endpoint. */
    suspend fun connect(remote: SocketEndpoint): ChatConnection {
        Napier.d("Connecting to remote endpoint: ${remote.host}:${remote.port}")
        val socket: Socket = withContext(dispatchers.io) {
            aSocket(selectorManager).tcp().connect(remote.host, remote.port)
        }
        Napier.d("Connected to remote endpoint: ${socket.remoteAddress}")
        return ChatConnection(socket, dispatchers)
    }

    /** Parse textual representation of an endpoint. */
    fun parse(text: String): SocketEndpoint? {
        val trimmed = text.trim()
        if (":" in trimmed && !trimmed.startsWith("ktlan://")) {
            val parts = trimmed.split(":", limit = 2)
            val port = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return SocketEndpoint(parts[0], port)
        }
        if (trimmed.startsWith("ktlan://")) {
            val query = trimmed.substringAfter('?')
            val params = mutableMapOf<String, String>()
            query.split('&').forEach { p ->
                val kv = p.split('=', limit = 2)
                if (kv.size == 2) params[kv[0]] = kv[1]
            }
            val host = params["host"] ?: return null
            val port = params["port"]?.toIntOrNull() ?: return null
            return SocketEndpoint(host, port)
        }
        return null
    }
}
