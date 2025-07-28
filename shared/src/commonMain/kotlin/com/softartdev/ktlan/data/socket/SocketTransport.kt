package com.softartdev.ktlan.data.socket

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.bind
import io.ktor.network.sockets.connect
import io.ktor.network.sockets.tcp
import io.ktor.network.sockets.tryClose
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simple TCP transport utilities.
 */
object SocketTransport {

    /** Represents an established chat connection. */
    class ChatConnection(private val socket: Socket) {
        private val input = socket.openReadChannel()
        private val output = socket.openWriteChannel(autoFlush = true)

        /** Stream of incoming text lines. */
        val incoming: Flow<String> = flow {
            while (!input.isClosedForRead) {
                val line = try {
                    input.readUTF8Line()
                } catch (_: CancellationException) {
                    null
                }
                line ?: break
                emit(line)
            }
        }

        /** Sends a single line. */
        suspend fun send(line: String) {
            output.writeStringUtf8(line)
            output.writeStringUtf8("\n")
        }

        /** Closes the connection. */
        suspend fun close() {
            socket.close()
        }
    }

    /** Starts a TCP server and waits for the first client connection. */
    suspend fun startServer(bindHost: String, bindPort: Int): Pair<ChatConnection, suspend () -> Unit> {
        val selector = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selector).tcp().bind(bindHost, bindPort)
        val client = serverSocket.accept()
        val connection = ChatConnection(client)
        val stop: suspend () -> Unit = {
            connection.close()
            serverSocket.tryClose()
            selector.close()
        }
        return connection to stop
    }

    /** Connects to a remote TCP server. */
    suspend fun connect(remote: SocketEndpoint): ChatConnection {
        val selector = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selector).tcp().connect(remote.host, remote.port)
        return ChatConnection(socket)
    }

    /** Parses either "host:port" or "ktlan://tcp?host=...&port=..." strings. */
    fun parseEndpoint(text: String): SocketEndpoint? {
        val trimmed = text.trim()
        if (trimmed.startsWith("ktlan://tcp")) {
            return runCatching {
                val url = io.ktor.http.Url(trimmed)
                val host = url.parameters["host"] ?: return null
                val port = url.parameters["port"]?.toIntOrNull() ?: return null
                SocketEndpoint(host, port)
            }.getOrNull()
        }
        val parts = trimmed.split(":")
        if (parts.size == 2) {
            val port = parts[1].toIntOrNull() ?: return null
            return SocketEndpoint(parts[0], port)
        }
        return null
    }
}
