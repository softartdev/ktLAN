package com.softartdev.ktlan.presentation.socket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
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
 * ViewModel for the socket chat screen.
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
        socketRepo.messages.onEach { message: ChatMessage ->
            mutableState.update { it.copy(messages = it.messages + message) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: SocketAction) = viewModelScope.launch {
        when (action) {
            is SocketAction.StartServer -> startServer(action.bindHost, action.bindPort)
            is SocketAction.Connect -> connect(action.remoteHost, action.remotePort)
            SocketAction.StopAll -> stopAll()
            is SocketAction.Send -> send(action.text)
            SocketAction.ShowQrForServer -> showQr()
            is SocketAction.ApplyQrPayload -> applyQrPayload(action.payload)
            is SocketAction.EditDraft -> mutableState.update { it.copy(draft = action.newText) }
            is SocketAction.SetBindHost -> mutableState.update { it.copy(bindHost = action.value) }
            is SocketAction.SetBindPort -> mutableState.update { it.copy(bindPort = action.value) }
            is SocketAction.SetRemoteHost -> mutableState.update { it.copy(remoteHost = action.value) }
            is SocketAction.SetRemotePort -> mutableState.update { it.copy(remotePort = action.value) }
        }
    }

    private suspend fun startServer(host: String, port: String) {
        mutableState.update { it.copy(loading = true, error = null) }
        runCatching { socketRepo.startServer(host, port.toInt()) }
            .onSuccess {
                mutableState.update { it.copy(loading = false, serverRunning = true, connected = true) }
            }
            .onFailure { e ->
                mutableState.update { it.copy(loading = false, error = e.message) }
            }
    }

    private suspend fun connect(host: String, port: String) {
        mutableState.update { it.copy(loading = true, error = null) }
        runCatching { socketRepo.connectTo(host, port.toInt()) }
            .onSuccess {
                mutableState.update { it.copy(loading = false, connected = true, serverRunning = false) }
            }
            .onFailure { e ->
                mutableState.update { it.copy(loading = false, error = e.message) }
            }
    }

    private suspend fun send(text: String) {
        socketRepo.send(text)
        mutableState.update { it.copy(draft = "") }
    }

    private suspend fun stopAll() {
        socketRepo.stop()
        mutableState.update { it.copy(serverRunning = false, connected = false) }
    }

    private fun showQr() {
        val state = mutableState.value
        val url = "ktlan://tcp?host=${state.bindHost}&port=${state.bindPort}&v=1"
        router.navigate(AppNavGraph.QrDialog(url))
    }

    private fun applyQrPayload(payload: String) {
        val endpoint = SocketTransport.parseEndpoint(payload)
        if (endpoint != null) {
            mutableState.update { it.copy(remoteHost = endpoint.host, remotePort = endpoint.port.toString()) }
        } else {
            mutableState.update { it.copy(error = "Invalid QR data") }
        }
    }
}
