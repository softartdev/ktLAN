package com.softartdev.ktlan.presentation.socket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.data.socket.SocketEndpoint
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.repo.SocketRepo
import com.softartdev.ktlan.domain.repo.NetworksRepo
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocketViewModel(
    private val router: Router,
    private val repo: SocketRepo,
    private val transport: SocketTransport,
    private val networksRepo: NetworksRepo,
    private val navParameters: AppNavGraph.BottomTab.Socket,
) : ViewModel() {
    private val state = MutableStateFlow(SocketResult())
    val stateFlow: StateFlow<SocketResult> = state
    private var launched = false

    fun launch() = viewModelScope.launch {
        if (launched) return@launch

        updateLocalIp()

        navParameters.bindHost?.let { bindHost ->
            state.update { it.copy(bindHost = bindHost) }
        }
        navParameters.remoteHost?.let { remoteHost ->
            state.update { it.copy(remoteHost = remoteHost) }
        }
        repo.observeMessages().onEach { msg: ChatMessage ->
            state.update { it.copy(messages = it.messages + msg) }
        }.launchIn(viewModelScope)

        launched = true
    }

    suspend fun updateLocalIp() {
        val ip = networksRepo.guessLocalIPv4() ?: repo.getLocalIp()
        state.update { it.copy(bindHost = ip) }
    }

    /** Handle user actions. */
    fun onAction(action: SocketAction) = when (action) {
        is SocketAction.StartServer -> viewModelScope.launch {
            startServer(action.bindHost, action.bindPort)
        }
        is SocketAction.Connect -> viewModelScope.launch {
            connect(action.remoteHost, action.remotePort)
        }
        is SocketAction.StopAll -> viewModelScope.launch { stopAll() }
        is SocketAction.Send -> viewModelScope.launch { send(action.text) }
        is SocketAction.ShowQrForServer -> showQr()
        is SocketAction.ApplyQrPayload -> applyQr(action.payload)
        is SocketAction.EditDraft -> state.update { it.copy(draft = action.newText) }
        is SocketAction.SetBindHost -> state.update { it.copy(bindHost = action.bindHost) }
        is SocketAction.SetBindPort -> state.update { it.copy(bindPort = action.bindPort) }
        is SocketAction.SetRemoteHost -> state.update { it.copy(remoteHost = action.remoteHost) }
        is SocketAction.SetRemotePort -> state.update { it.copy(remotePort = action.remotePort) }
    }

    suspend fun startServer(host: String, portString: String) {
        Napier.d("Starting server on $host:$portString")
        val port: Int? = portString.toIntOrNull()
        if (port == null) {
            state.update { it.copy(error = "Invalid port") }
            return
        }
        state.update { it.copy(loading = true, error = null) }
        runCatching { repo.startServer(host, port) }
            .onSuccess {
                Napier.d("Server started successfully on $host:$portString")
                state.update { it.copy(loading = false, serverRunning = true, connected = true) }
            }
            .onFailure { e ->
                Napier.e("Failed to start server", e)
                state.update { it.copy(loading = false, error = e.message) }
            }
    }

    suspend fun connect(host: String, portString: String) {
        Napier.d("Connecting to $host:$portString")
        val port: Int? = portString.toIntOrNull()
        if (port == null) {
            state.update { it.copy(error = "Invalid port") }
            Napier.e("Invalid port: $portString")
            return
        }
        state.update { it.copy(loading = true, error = null) }
        runCatching { repo.connectTo(host, port) }
            .onSuccess {
                Napier.d("Connected to $host:$portString")
                state.update { it.copy(loading = false, connected = true) }
            }
            .onFailure { e ->
                Napier.e("Failed to connect to $host:$portString", e)
                state.update { it.copy(loading = false, error = e.message) }
            }
    }

    suspend fun send(text: String) {
        repo.send(text)
        state.update { it.copy(draft = "") }
    }

    suspend fun stopAll() {
        repo.stop()
        state.update { it.copy(serverRunning = false, connected = false) }
    }

    fun showQr() {
        val current: SocketResult = state.value
        val url = "ktlan://tcp?host=${current.bindHost}&port=${current.bindPort}&v=1"
        router.navigate(AppNavGraph.QrDialog(url))
    }

    fun applyQr(payload: String) = state.update { result: SocketResult ->
        val endpoint: SocketEndpoint? = transport.parse(payload)
        return@update when {
            endpoint != null -> result.copy(
                remoteHost = endpoint.host,
                remotePort = endpoint.port.toString(),
                error = null
            )
            else -> result.copy(error = "Invalid QR data")
        }
    }
}
