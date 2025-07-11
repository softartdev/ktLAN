package com.softartdev.ktlan.presentation.connect

import com.softartdev.ktlan.data.webrtc.P2pState
import com.softartdev.ktlan.domain.model.ConsoleMessage

data class ConnectResult(
    val consoleMessages: List<ConsoleMessage> = emptyList(),
    val loading: Boolean = false,
    val inputEnabled: Boolean = true,
    val createOfferVisible: Boolean = false,
    val p2pState: P2pState? = null
)

sealed interface ConnectAction {
    data class Submit(val message: String) : ConnectAction
    data object CreateOffer : ConnectAction
    data class ShowQr(val text: String) : ConnectAction
    data class PrintError(val exception: Exception) : ConnectAction
    data class PrintConsole(val message: String) : ConnectAction
}
