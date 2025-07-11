package com.softartdev.ktlan.data.webrtc

import org.json.JSONException
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import java.nio.charset.Charset

open class DefaultDataChannelObserver(
    val channel: DataChannel,
    val UTF_8: Charset,
    val console: IConsole,
    val setState: (P2pState) -> Unit,
    val JSON_MESSAGE: String,
    val pc: PeerConnection
) : DataChannel.Observer {
    //TODO I'm not sure if this would handle really long messages
    override fun onMessage(p0: DataChannel.Buffer?) {
        val buf = p0?.data
        if (buf != null) {
            val byteArray = ByteArray(buf.remaining())
            buf.get(byteArray)
            val received = String(byteArray, UTF_8)
            try {
                val message = JSONObject(received).getString(JSON_MESSAGE)
                console.bluef("&gt;$message")
            } catch (e: JSONException) {
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
            setState(P2pState.CHAT_ESTABLISHED)
            console.bluef("Chat established.")
            // print to console current connection like "ip(1):port1 -> ... -> ip(N):port"
            val remoteAddress = pc.remoteDescription?.description ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        } else {
            setState(P2pState.CHAT_ENDED)
            console.redf("Chat ended.")
        }
    }
}