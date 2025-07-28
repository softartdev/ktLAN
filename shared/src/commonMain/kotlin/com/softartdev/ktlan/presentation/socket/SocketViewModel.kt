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

/** ViewModel orchestrating socket chat operations. */
class SocketViewModel(
    private val router: Router,
    private val repo: SocketRepo,
    private val transport: SocketTransport
) : ViewModel() {
    private val state = MutableStateFlow(SocketResult())
    val stateFlow: StateFlow<SocketResult> = state
    private var launched = false

    /** Subscribe to repo and prefill local IP. */
    fun launch() {
        if (launched) return
        launched = true
        viewModelScope.launch {
            val ip = repo.getLocalIp()
            state.update { it.copy(bindHost = ip) }
        }
        repo.observeMessages().onEach { msg ->
            state.update { it.copy(messages = it.messages + msg) }
        }.launchIn(viewModelScope)
    }

    /** Handle user actions. */
    fun onAction(action: SocketAction) = when (action) {
        is SocketAction.StartServer -> startServer(action.bindHost, action.bindPort)
        is SocketAction.Connect -> connect(action.remoteHost, action.remotePort)
        is SocketAction.StopAll -> stopAll()
        is SocketAction.Send -> send(action.text)
        is SocketAction.ShowQrForServer -> showQr()
        is SocketAction.ApplyQrPayload -> applyQr(action.payload)
        is SocketAction.EditDraft -> state.update { it.copy(draft = action.newText) }
        is SocketAction.SetBindHost -> state.update { it.copy(bindHost = action.bindHost) }
        is SocketAction.SetBindPort -> state.update { it.copy(bindPort = action.bindPort) }
        is SocketAction.SetRemoteHost -> state.update { it.copy(remoteHost = action.remoteHost) }
        is SocketAction.SetRemotePort -> state.update { it.copy(remotePort = action.remotePort) }
    }

    private fun startServer(host: String, portString: String) = viewModelScope.launch {
        val port = portString.toIntOrNull()
        if (port == null) {
            state.update { it.copy(error = "Invalid port") }
            return@launch
        }
        state.update { it.copy(loading = true, error = null) }
        runCatching { repo.startServer(host, port) }
            .onSuccess {
                state.update { it.copy(loading = false, serverRunning = true, connected = true) }
            }
            .onFailure { e ->
                state.update { it.copy(loading = false, error = e.message) }
            }
    }

    private fun connect(host: String, portString: String) = viewModelScope.launch {
        val port = portString.toIntOrNull()
        if (port == null) {
            state.update { it.copy(error = "Invalid port") }
            return@launch
        }
        state.update { it.copy(loading = true, error = null) }
        runCatching { repo.connectTo(host, port) }
            .onSuccess {
                state.update { it.copy(loading = false, connected = true) }
            }
            .onFailure { e ->
                state.update { it.copy(loading = false, error = e.message) }
            }
    }

    private fun send(text: String) = viewModelScope.launch {
        repo.send(text)
        state.update { it.copy(draft = "") }
    }

    private fun stopAll() = viewModelScope.launch {
        repo.stop()
        state.update { it.copy(serverRunning = false, connected = false) }
    }

    private fun showQr() {
        val current = state.value
        val url = "ktlan://tcp?host=${current.bindHost}&port=${current.bindPort}&v=1"
        router.navigate(AppNavGraph.QrDialog(url))
    }

    private fun applyQr(payload: String) {
        val endpoint = transport.parse(payload)
        if (endpoint != null) {
            state.update { it.copy(remoteHost = endpoint.host, remotePort = endpoint.port.toString()) }
        } else {
            state.update { it.copy(error = "Invalid QR data") }
        }
    }
}
