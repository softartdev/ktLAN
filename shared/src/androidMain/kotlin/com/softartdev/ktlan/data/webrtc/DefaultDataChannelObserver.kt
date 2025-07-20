package com.softartdev.ktlan.data.webrtc

import org.json.JSONException
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import java.nio.charset.Charset

open class DefaultDataChannelObserver(
    val channel: DataChannel,
    val charset: Charset,
    val console: IConsole,
    val setState: (P2pState) -> Unit,
    val jsonMessage: String,
    val pc: PeerConnection
) : DataChannel.Observer {

    override fun onMessage(p0: DataChannel.Buffer?) {
        val buf = p0?.data
        if (buf != null) {
            val byteArray = ByteArray(buf.remaining())
            buf.get(byteArray)
            val received = String(byteArray, charset)
            try {
                val message = JSONObject(received).getString(jsonMessage)
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
            val remoteAddress = pc.remoteDescription?.description ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        } else {
            setState(P2pState.CHAT_ENDED)
            console.redf("Chat ended.")
        }
    }
}