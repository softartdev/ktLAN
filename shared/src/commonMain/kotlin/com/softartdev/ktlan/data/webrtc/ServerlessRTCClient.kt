package com.softartdev.ktlan.data.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ServerlessRTCClient(
    var console: IConsole = LogConsole(),
) {
    private val p2pMutableStateFlow: MutableStateFlow<P2pState?> = MutableStateFlow(null)
    val p2pStateFlow: StateFlow<P2pState?> = p2pMutableStateFlow

    var p2pState: P2pState?
        internal set(value) {
            p2pMutableStateFlow.value = value
        }
        get() = p2pStateFlow.value
    /**
     * Call this before using anything else from PeerConnection.
     */
    abstract fun init()

    /**
     * Wait for an offer to be entered by user.
     */
    fun waitForOffer() {
        p2pState = P2pState.WAITING_FOR_OFFER
    }

    /**
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    abstract fun processOffer(sdpJSON: String)

    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    abstract fun processAnswer(sdpJSON: String)

    /**
     * App creates the offer.
     */
    abstract fun makeOffer()

    /**
     * Sends message to other party.
     */
    abstract fun sendMessage(message: String)

    /**
     * Creates data channel for use when offer is created on this machine.
     */
    abstract fun makeDataChannel()

    /**
     * Clean up some resources.
     */
    abstract fun destroy()

    companion object {
        internal const val JSON_TYPE: String = "type"
        internal const val JSON_MESSAGE: String = "message"
        internal const val JSON_SDP: String = "sdp"
    }
}