package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.RTCDataChannelBuffer
import dev.onvoid.webrtc.RTCDataChannelObserver
import dev.onvoid.webrtc.RTCDataChannelState
import dev.onvoid.webrtc.RTCPeerConnection
import java.nio.ByteBuffer

open class DefaultDataChannelObserver(
    private val channel: RTCDataChannel,
    private val rtcClient: ServerlessRTCClient,
    private val pc: RTCPeerConnection,
    private val console: IConsole = rtcClient.console,
) : RTCDataChannelObserver {

    override fun onMessage(buffer: RTCDataChannelBuffer?) {
        val buf: ByteBuffer? = buffer?.data
        if (buf != null) {
            val byteArray = ByteArray(buf.remaining())
            buf.get(byteArray)
            val received = String(byteArray, Charsets.UTF_8)
            val message: String? = ServerlessRTCClient.deserializeMessage(received, console)
            if (message != null) {
                console.bluef(">$message")
            } else {
                console.redf("Malformed message received")
            }
        }
    }

    override fun onBufferedAmountChange(previousAmount: Long) {
        console.d("channel buffered amount change:{$previousAmount}")
    }

    override fun onStateChange() {
        console.d("Channel state changed:${channel.state.name}")
        if (channel.state == RTCDataChannelState.OPEN) {
            rtcClient.p2pState = P2pState.CHAT_ESTABLISHED
            console.bluef("Chat established.")
            val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        } else if (channel.state == RTCDataChannelState.CLOSED) {
            rtcClient.p2pState = P2pState.CHAT_ENDED
            console.redf("Chat ended.")
        }
    }
}
