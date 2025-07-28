package com.softartdev.ktlan.data.socket

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Minimal TCP transport based on Ktor sockets.
 */
object SocketTransport {

    /**
     * Represents an active chat connection.
     */
    class ChatConnection internal constructor(
        private val socket: Socket,
        private val closeable: Closeable? = null,
    ) {
        private val reader = socket.openReadChannel()
        private val writer = socket.openWriteChannel(autoFlush = true)

        /** Incoming text lines from the peer. */
        val incoming: Flow<String> = flow {
            while (true) {
                val line: String = reader.readUTF8Line() ?: break
                emit(line)
            }
        }

        /** Sends a single line of text. */
        suspend fun send(line: String) {
            writer.writeStringUtf8(line + "\n")
        }

        /** Closes the connection and associated resources. */
        suspend fun close() {
            try {
                socket.close()
            } finally {
                closeable?.close()
            }
        }
    }

    /**
     * Starts a TCP server and waits for the first incoming connection.
     * Returns a pair of stop handle and opened [ChatConnection].
     */
    suspend fun startServer(bindHost: String, bindPort: Int): Pair<suspend () -> Unit, ChatConnection> {
        val selector = SelectorManager(Dispatchers.Default)
        val server = aSocket(selector).tcp().bind(bindHost, bindPort)
        val socket = server.accept()
        val stop: suspend () -> Unit = {
            server.close()
            selector.close()
        }
        return stop to ChatConnection(socket)
    }

    /** Connects to the given remote endpoint. */
    suspend fun connect(remote: SocketEndpoint): ChatConnection {
        val selector = SelectorManager(Dispatchers.Default)
        val socket = aSocket(selector).tcp().connect(remote.host, remote.port)
        return ChatConnection(socket, selector)
    }

    /**
     * Parses "ip:port" or "ktlan://tcp?host=...&port=..." strings.
     */
    fun parseEndpoint(text: String): SocketEndpoint? {
        val trimmed = text.trim()
        if (trimmed.startsWith("ktlan://tcp")) {
            val query = trimmed.substringAfter('?', "")
            val params = query.split('&').mapNotNull { part ->
                val kv = part.split('=', limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }.toMap()
            val host = params["host"]
            val port = params["port"]?.toIntOrNull()
            if (host != null && port != null) return SocketEndpoint(host, port)
        }
        if (':' in trimmed) {
            val (host, portStr) = trimmed.split(':', limit = 2)
            val port = portStr.toIntOrNull() ?: return null
            return SocketEndpoint(host, port)
        }
        return null
    }
}
