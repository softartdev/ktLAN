package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.RTCDataChannelBuffer
import dev.onvoid.webrtc.RTCDataChannelObserver
import java.nio.ByteBuffer
import java.nio.charset.Charset

open class DefaultDataChannelObserver(
    private val channel: RTCDataChannel,
    private val charset: Charset,
    private val console: IConsole,
    private val setState: (P2pState) -> Unit,
    private val jsonMessage: String,
    private val pc: dev.onvoid.webrtc.RTCPeerConnection
) : RTCDataChannelObserver {

    override fun onMessage(buffer: RTCDataChannelBuffer?) {
        val buf: ByteBuffer? = buffer?.data
        if (buf != null) {
            val byteArray = ByteArray(buf.remaining())
            buf.get(byteArray)
            val received = String(byteArray, charset)
            val pattern = Regex("""\"$jsonMessage\"\s*:\s*\"([^\"]*)\"""")
            val message = pattern.find(received)?.groupValues?.getOrNull(1)
            if (message != null) {
                console.bluef("&gt;$message")
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
        if (channel.state == dev.onvoid.webrtc.RTCDataChannelState.OPEN) {
            setState(P2pState.CHAT_ESTABLISHED)
            console.bluef("Chat established.")
            val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        } else if (channel.state == dev.onvoid.webrtc.RTCDataChannelState.CLOSED) {
            setState(P2pState.CHAT_ENDED)
            console.redf("Chat ended.")
        }
    }
}
