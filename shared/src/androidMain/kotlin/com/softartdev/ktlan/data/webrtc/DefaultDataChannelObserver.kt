package com.softartdev.ktlan.data.webrtc

import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import java.nio.ByteBuffer

open class DefaultDataChannelObserver(
    val channel: DataChannel,
    val rtcClient: ServerlessRTCClient,
    val pc: PeerConnection,
    val console: IConsole = rtcClient.console,
) : DataChannel.Observer {

    override fun onMessage(p0: DataChannel.Buffer?) {
        val buf: ByteBuffer? = p0?.data
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

    override fun onBufferedAmountChange(p0: Long) {
        console.d("channel buffered amount change:{$p0}")
    }

    override fun onStateChange() {
        console.d("Channel state changed:${channel.state()?.name}}")
        if (channel.state() == DataChannel.State.OPEN) {
            rtcClient.p2pState = P2pState.CHAT_ESTABLISHED
            console.bluef("Chat established.")
            val remoteAddress = pc.remoteDescription?.description ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        } else {
            rtcClient.p2pState = P2pState.CHAT_ENDED
            console.redf("Chat ended.")
        }
    }
}