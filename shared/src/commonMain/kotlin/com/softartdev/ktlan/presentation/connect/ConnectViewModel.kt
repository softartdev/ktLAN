@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.presentation.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.data.webrtc.P2pState
import com.softartdev.ktlan.data.webrtc.P2pState.CHAT_ESTABLISHED
import com.softartdev.ktlan.data.webrtc.P2pState.CREATING_ANSWER
import com.softartdev.ktlan.data.webrtc.P2pState.CREATING_OFFER
import com.softartdev.ktlan.data.webrtc.P2pState.WAITING_FOR_ANSWER
import com.softartdev.ktlan.data.webrtc.P2pState.WAITING_FOR_OFFER
import com.softartdev.ktlan.data.webrtc.P2pState.WAITING_TO_CONNECT
import com.softartdev.ktlan.domain.model.ConsoleMessage
import com.softartdev.ktlan.domain.repo.ConnectRepo
import com.softartdev.ktlan.presentation.navigation.AppNavGraph.QrDialog
import com.softartdev.ktlan.presentation.navigation.Router
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ConnectViewModel(
    private val router: Router,
    private val connectRepo: ConnectRepo
) : ViewModel() {
    private val logger = Logger.withTag("ConnectViewModel")
    private val mutableStateFlow = MutableStateFlow(value = ConnectResult())
    val stateFlow: StateFlow<ConnectResult> = mutableStateFlow
    private var launched: Boolean = false

    fun launch() {
        logger.d { "launched = $launched" }
        if (launched) return
        launched = true

        connectRepo.flow.onEach { consoleMessage: ConsoleMessage ->
            mutableStateFlow.update { connectResult: ConnectResult ->
                val messages = connectResult.consoleMessages + consoleMessage
                return@update connectResult.copy(consoleMessages = messages)
            }
        }.launchIn(viewModelScope)

        connectRepo.p2pStateFlow.onEach { state: P2pState? ->
            val inputEnabled: Boolean = when (state) {
                CREATING_OFFER, CREATING_ANSWER, WAITING_TO_CONNECT -> false
                else -> true
            }
            mutableStateFlow.update { connectResult: ConnectResult ->
                return@update connectResult.copy(
                    loading = !inputEnabled,
                    inputEnabled = inputEnabled,
                    createOfferVisible = state == WAITING_FOR_OFFER,
                    p2pState = state,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: ConnectAction) = viewModelScope.launch {
        val result: ConnectResult = stateFlow.value
        try {
            mutableStateFlow.value = result.copy(loading = true)
            when (action) {
                is ConnectAction.CreateOffer -> connectRepo.makeOffer()
                is ConnectAction.Submit -> when (connectRepo.p2pStateFlow.value) {
                    WAITING_FOR_OFFER -> connectRepo.processOffer(sdpJSON = action.message)
                    WAITING_FOR_ANSWER -> connectRepo.processAnswer(sdpJSON = action.message)
                    CHAT_ESTABLISHED -> connectRepo.sendMessage(message = action.message)
                    else -> connectRepo.webRtcClient.console.printf(text = action.message)
                }
                is ConnectAction.ShowQr -> router.navigate(
                    route = QrDialog(text = action.text)
                )
                is ConnectAction.PrintError -> connectRepo.webRtcClient.console.error(
                    action.exception.message ?: "Unknown error"
                )
                is ConnectAction.PrintConsole -> connectRepo.webRtcClient.console.debug(action.message)
            }
            mutableStateFlow.value = result.copy(loading = false)
        } catch (throwable: Throwable) {
            logger.e(throwable) { "Error processing action: $action" }
            val consoleMessage = ConsoleMessage(
                leading = "❌",
                overline = Clock.System.now().toString(),
                headline = "Error processing action: $action",
                supporting = throwable.message ?: "Unknown error",
                trailing = "🔴"
            )
            mutableStateFlow.value = result.copy(
                consoleMessages = result.consoleMessages + consoleMessage
            )
        } finally {
            mutableStateFlow.value = result.copy(loading = false)
        }
    }
}
