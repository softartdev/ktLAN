package com.softartdev.ktlan.presentation.socket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.repo.SocketRepo
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel bridging UI and [SocketRepo].
 */
class SocketViewModel(
    private val router: Router,
    private val socketRepo: SocketRepo,
) : ViewModel() {

    private val mutableState = MutableStateFlow(SocketResult())
    val stateFlow: StateFlow<SocketResult> = mutableState
    private var launched = false

    fun launch() {
        if (launched) return
        launched = true
        socketRepo.messages.onEach { message ->
            mutableState.update { it.copy(messages = it.messages + message) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: SocketAction) = viewModelScope.launch {
        when (action) {
            is SocketAction.StartServer -> startServer(action.bindHost, action.bindPort)
            is SocketAction.StopAll -> stopAll()
            is SocketAction.Connect -> connect(action.remoteHost, action.remotePort)
            is SocketAction.Send -> sendMessage(action.text)
            SocketAction.ShowQrForServer -> showQr()
            is SocketAction.ApplyQrPayload -> applyQr(action.payload)
            is SocketAction.EditDraft -> mutableState.update { it.copy(draft = action.newText) }
            is SocketAction.SetBindHost -> mutableState.update { it.copy(bindHost = action.bindHost) }
            is SocketAction.SetBindPort -> mutableState.update { it.copy(bindPort = action.bindPort) }
            is SocketAction.SetRemoteHost -> mutableState.update { it.copy(remoteHost = action.remoteHost) }
            is SocketAction.SetRemotePort -> mutableState.update { it.copy(remotePort = action.remotePort) }
        }
    }

    private suspend fun startServer(host: String, portText: String) {
        val port = portText.toIntOrNull()
        if (port == null) {
            mutableState.update { it.copy(error = "Invalid port") }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            socketRepo.startServer(host, port)
            mutableState.update { it.copy(loading = false, serverRunning = true, connected = true, bindHost = host, bindPort = portText) }
        } catch (t: Throwable) {
            mutableState.update { it.copy(loading = false, error = t.message) }
        }
    }

    private suspend fun connect(host: String, portText: String) {
        val port = portText.toIntOrNull()
        if (port == null) {
            mutableState.update { it.copy(error = "Invalid port") }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            socketRepo.connectTo(host, port)
            mutableState.update { it.copy(loading = false, connected = true, remoteHost = host, remotePort = portText) }
        } catch (t: Throwable) {
            mutableState.update { it.copy(loading = false, error = t.message) }
        }
    }

    private suspend fun sendMessage(text: String) {
        if (text.isBlank()) return
        socketRepo.send(text)
        mutableState.update { it.copy(draft = "") }
    }

    private suspend fun stopAll() {
        socketRepo.stop()
        mutableState.update { it.copy(serverRunning = false, connected = false) }
    }

    private fun showQr() {
        val state = stateFlow.value
        val url = "ktlan://tcp?host=${state.bindHost}&port=${state.bindPort}&v=1"
        router.navigate(AppNavGraph.QrDialog(url))
    }

    private fun applyQr(payload: String) {
        val endpoint = SocketTransport.parseEndpoint(payload)
        if (endpoint == null) {
            mutableState.update { it.copy(error = "Invalid QR") }
        } else {
            mutableState.update { it.copy(remoteHost = endpoint.host, remotePort = endpoint.port.toString()) }
        }
    }
}
