package com.softartdev.ktlan.data.webrtc

import org.webrtc.DataChannel
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

abstract class DefaultObserver(
    private val console: IConsole,
    private val channel: DataChannel?
) : PeerConnection.Observer {
    override fun onDataChannel(p0: DataChannel?) {
        console.d("data channel ${p0?.label()} established")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        console.d("ice connection receiving change:{$p0}")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        console.d("ice connection state change:${p0?.name}")
        if (p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
            console.d("closing channel")
            channel?.close()
        }
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        console.d("ice gathering state change:${p0?.name}")
    }

    override fun onAddStream(p0: MediaStream?) {}

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        console.d("signaling state change:${p0?.name}")
    }

    override fun onRemoveStream(p0: MediaStream?) {}

    override fun onRenegotiationNeeded() {
        console.d("renegotiation needed")
    }
}