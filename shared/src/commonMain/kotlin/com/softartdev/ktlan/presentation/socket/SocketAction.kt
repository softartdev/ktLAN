package com.softartdev.ktlan.presentation.socket

/** Actions emitted from the socket UI. */
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
