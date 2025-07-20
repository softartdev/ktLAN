package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.PeerConnectionObserver
import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.RTCIceGatheringState
import dev.onvoid.webrtc.RTCIceConnectionState
import dev.onvoid.webrtc.RTCSignalingState
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.media.MediaStream

/** Observer implementation for desktop WebRTC peer connection. */
open class DefaultObserver(
    private val console: IConsole,
    private val channel: RTCDataChannel?
) : PeerConnectionObserver {
    override fun onDataChannel(dataChannel: RTCDataChannel?) {
        console.d("data channel ${dataChannel?.label} established")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        console.d("ice connection receiving change:{$receiving}")
    }

    override fun onIceConnectionChange(state: RTCIceConnectionState?) {
        console.d("ice connection state change:${state?.name}")
        if (state == RTCIceConnectionState.DISCONNECTED) {
            console.d("closing channel")
            channel?.close()
        }
    }

    override fun onIceCandidate(candidate: RTCIceCandidate?) {
        console.d("ice candidate:{${candidate?.sdp}}")
    }

    override fun onIceGatheringChange(state: RTCIceGatheringState?) {
        console.d("ice gathering state change:${state?.name}")
    }

    override fun onAddStream(stream: MediaStream?) {
        console.d("stream added: $stream")
    }

    override fun onSignalingChange(state: RTCSignalingState?) {
        console.d("signaling state change:${state?.name}")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        console.d("stream removed: $stream")
    }

    override fun onRenegotiationNeeded() {
        console.d("renegotiation needed")
    }
}
