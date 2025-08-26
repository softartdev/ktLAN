@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.presentation.connect

import com.softartdev.ktlan.data.webrtc.P2pState
import com.softartdev.ktlan.domain.model.ConsoleMessage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ConnectResult(
    val consoleMessages: List<ConsoleMessage> = emptyList(),
    val loading: Boolean = false,
    val inputEnabled: Boolean = true,
    val createOfferVisible: Boolean = false,
    val p2pState: P2pState? = null
) {
    companion object {
        val previewMessages: List<ConsoleMessage>
            get() = P2pState.entries.map { p2pState ->
                ConsoleMessage(
                    leading = "🔔",
                    overline = Clock.System.now().toString(),
                    headline = p2pState.name,
                    supporting = "This is a sample message for state ${p2pState.name}",
                    trailing = "🦄"
                )
            }
    }
}

sealed interface ConnectAction {
    data class Submit(val message: String) : ConnectAction
    data object CreateOffer : ConnectAction
    data class ShowQr(val text: String) : ConnectAction
    data class PrintError(val exception: Exception) : ConnectAction
    data class PrintConsole(val message: String) : ConnectAction
}
