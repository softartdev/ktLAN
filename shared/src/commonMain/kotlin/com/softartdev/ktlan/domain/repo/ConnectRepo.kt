@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.webrtc.P2pState
import com.softartdev.ktlan.data.webrtc.P2pState.CHAT_ENDED
import com.softartdev.ktlan.data.webrtc.P2pState.INITIALIZING
import com.softartdev.ktlan.data.webrtc.ServerlessRTCClient
import com.softartdev.ktlan.domain.model.ConsoleMessage
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * This repo define data source type for emiting items of [com.softartdev.ktlan.domain.model.ConsoleMessage] type.
 */
class ConnectRepo(
    val webRtcClient: ServerlessRTCClient,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatchers.default)

    /**
     * Mutable state flow to hold the current console message.
     */
    private val mutableSharedFlow: MutableSharedFlow<ConsoleMessage> = MutableSharedFlow(
        replay = 3,
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val flow: SharedFlow<ConsoleMessage> = mutableSharedFlow
    val p2pStateFlow: StateFlow<P2pState?> = webRtcClient.p2pStateFlow

    init {
        webRtcClient.p2pStateFlow.onEach { p2pState: P2pState? ->
            val consoleMessage = ConsoleMessage(
                leading = "🔔",
                overline = Clock.System.now().toString(),
                headline = p2pState?.name ?: "NULL STATE",
                trailing = "🦄"
            )
            mutableSharedFlow.emit(consoleMessage)

            if (p2pState == INITIALIZING || p2pState == CHAT_ENDED) {
                waitForOffer()
            }
        }.launchIn(coroutineScope)

        webRtcClient.console = SharedFlowConsole(mutableSharedFlow, coroutineDispatchers)
    }

    fun waitForOffer() = webRtcClient.waitForOffer()

    fun processOffer(sdpJSON: String) = webRtcClient.processOffer(sdpJSON)

    fun processAnswer(sdpJSON: String) = webRtcClient.processAnswer(sdpJSON)

    fun makeOffer() = webRtcClient.makeOffer()

    fun sendMessage(message: String) = webRtcClient.sendMessage(message)

    fun makeDataChannel() = webRtcClient.makeDataChannel()

    fun destroy() = webRtcClient.destroy()
}