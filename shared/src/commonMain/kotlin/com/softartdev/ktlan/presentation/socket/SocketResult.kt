package com.softartdev.ktlan.presentation.socket

import com.softartdev.ktlan.domain.model.ChatMessage

/** ViewModel state for socket chat screen. */
data class SocketResult(
    val loading: Boolean = false,
    val serverRunning: Boolean = false,
    val connected: Boolean = false,
    val bindHost: String = "0.0.0.0",
    val bindPort: String = "51337",
    val remoteHost: String = "",
    val remotePort: String = "51337",
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val error: String? = null
) {
    companion object {
        val previewMessages: List<ChatMessage>
            get() = listOf(
                ChatMessage(
                    sender = ChatMessage.Sender.Local,
                    text = "Hello",
                    timestamp = 0
                ),
                ChatMessage(
                    sender = ChatMessage.Sender.Remote,
                    text = "Hi there!",
                    timestamp = 0
                ),
                ChatMessage(
                    sender = ChatMessage.Sender.Local,
                    text = "How are you?",
                    timestamp = 0
                )
            )
    }
}

sealed interface SocketAction {
    data class StartServer(val bindHost: String, val bindPort: String) : SocketAction
    data object StopAll : SocketAction
    data class Connect(val remoteHost: String, val remotePort: String) : SocketAction
    data class Send(val text: String) : SocketAction
    data object ShowQrForServer : SocketAction
    data class ApplyQrPayload(val payload: String) : SocketAction
    data class EditDraft(val newText: String) : SocketAction
    data class SetBindHost(val bindHost: String) : SocketAction
    data class SetBindPort(val bindPort: String) : SocketAction
    data class SetRemoteHost(val remoteHost: String) : SocketAction
    data class SetRemotePort(val remotePort: String) : SocketAction
}
