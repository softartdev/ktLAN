package com.softartdev.ktlan.data.webrtc

import kotlin.js.Promise

@JsFun("() => ({iceServers: [{urls: 'stun:stun.l.google.com:19302'}]})")
private external fun defaultRtcConfig(): RTCConfiguration

@JsFun("(type, sdp) => ({type: type, sdp: sdp})")
private external fun createSdp(type: String, sdp: String): RTCSessionDescriptionInit

@JsFun("() => ({})")
private external fun emptyDataChannelInit(): RTCDataChannelInit

/** External declarations for WebRTC browser API */
external class RTCPeerConnection(configuration: RTCConfiguration = definedExternally) : JsAny {
    var onicecandidate: ((RTCPeerConnectionIceEvent) -> Unit)?
    var onicegatheringstatechange: (() -> Unit)?
    var ondatachannel: ((RTCDataChannelEvent) -> Unit)?
    val localDescription: RTCSessionDescription?
    val remoteDescription: RTCSessionDescription?
    val iceGatheringState: String
    fun createOffer(): Promise<RTCSessionDescriptionInit>
    fun createAnswer(): Promise<RTCSessionDescriptionInit>
    fun setLocalDescription(desc: RTCSessionDescriptionInit): Promise<JsAny?>
    fun setRemoteDescription(desc: RTCSessionDescriptionInit): Promise<JsAny?>
    fun createDataChannel(label: String, options: RTCDataChannelInit = definedExternally): RTCDataChannel
    fun close()
}
external interface RTCConfiguration : JsAny { var iceServers: JsArray<RTCIceServer>? }
external interface RTCIceServer : JsAny { var urls: String }

external class RTCPeerConnectionIceEvent : JsAny { val candidate: RTCIceCandidate? }
external class RTCIceCandidate : JsAny { val candidate: String? }

external class RTCDataChannelEvent : JsAny { val channel: RTCDataChannel }

external class RTCDataChannel : JsAny {
    var onmessage: ((MessageEvent) -> Unit)?
    var onopen: (() -> Unit)?
    var onclose: (() -> Unit)?
    var onbufferedamountlow: (() -> Unit)?
    val bufferedAmount: Int
    fun send(data: String)
    fun close()
}

external interface RTCDataChannelInit : JsAny

external class RTCSessionDescription : JsAny { val type: String; val sdp: String }
external interface RTCSessionDescriptionInit : JsAny { var type: String; var sdp: String }

external class MessageEvent : JsAny { val data: JsAny? }

/**
 * WebRTC client implementation for wasm/js platform using browser WebRTC APIs.
 */
class WasmJsClientWebRTC : ServerlessRTCClient() {
    private var pc: RTCPeerConnection? = null
    private var channel: RTCDataChannel? = null

    override fun processOffer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.CREATING_ANSWER
        if (type == "offer" && sdp != null) {
            val peer = RTCPeerConnection(defaultRtcConfig())
            pc = peer
            peer.onicecandidate = { event: RTCPeerConnectionIceEvent ->
                console.d("ice candidate:{${event.candidate?.candidate}}")
            }
            peer.onicegatheringstatechange = {
                if (peer.iceGatheringState == "complete") {
                    console.printf("Here is your answer:")
                    val localDescription: RTCSessionDescription? = peer.localDescription
                    val sdpJSON: String? = localDescription?.let { desc: RTCSessionDescription ->
                        serializeSdp(type = desc.type, sdp = desc.sdp)
                    }
                    sdpJSON?.let(console::greenf)
                    p2pState = P2pState.WAITING_TO_CONNECT
                }
            }
            peer.ondatachannel = { evt ->
                val dc = evt.channel
                channel = dc
                registerDataChannel(dc)
            }
            peer.setRemoteDescription(createSdp(type, sdp)).then {
                peer.createAnswer().then(peer::setLocalDescription)
            }
        } else {
            console.redf("Invalid or unsupported offer.")
            p2pState = P2pState.WAITING_FOR_OFFER
        }
    }

    override fun processAnswer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.WAITING_TO_CONNECT
        val peer: RTCPeerConnection? = pc
        if (peer != null && type == "answer" && sdp != null) {
            peer.setRemoteDescription(createSdp(type, sdp))
        } else {
            console.redf("Invalid or unsupported answer.")
            p2pState = P2pState.WAITING_FOR_ANSWER
        }
    }

    override fun makeOffer() {
        p2pState = P2pState.CREATING_OFFER
        val peer = RTCPeerConnection(defaultRtcConfig())
        pc = peer
        peer.onicecandidate = { event ->
            console.d("ice candidate:{${event.candidate?.candidate}}")
        }
        peer.onicegatheringstatechange = {
            if (peer.iceGatheringState == "complete") {
                console.printf("Your offer is:")
                val localDescription: RTCSessionDescription? = peer.localDescription
                val sdpJSON: String? = localDescription?.let { desc: RTCSessionDescription ->
                    serializeSdp(type = desc.type, sdp = desc.sdp)
                }
                sdpJSON?.let(console::greenf)
                p2pState = P2pState.WAITING_FOR_ANSWER
            }
        }
        makeDataChannel()
        peer.createOffer().then(peer::setLocalDescription)
    }

    override fun sendMessage(message: String) {
        val dc: RTCDataChannel? = channel
        if (dc != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val json: String = serializeMessage(message)
            dc.send(json)
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    override fun makeDataChannel() {
        val peer: RTCPeerConnection = pc ?: return
        val dc: RTCDataChannel = peer.createDataChannel("test", emptyDataChannelInit())
        channel = dc
        registerDataChannel(dc)
    }

    private fun registerDataChannel(dc: RTCDataChannel) {
        dc.onmessage = { event: MessageEvent ->
            val json: String = event.data?.toString().orEmpty()
            val msg: String? = deserializeMessage(json, console)
            if (msg != null) {
                console.bluef(">$msg")
            } else {
                console.redf("Malformed message received")
            }
        }
        dc.onopen = {
            p2pState = P2pState.CHAT_ESTABLISHED
            console.bluef("Chat established.")
            val remoteAddress = pc?.remoteDescription?.sdp ?: "unknown"
            console.printf("Connected to remote peer: $remoteAddress")
        }
        dc.onclose = {
            p2pState = P2pState.CHAT_ENDED
            console.redf("Chat ended.")
        }
        dc.onbufferedamountlow = {
            console.d("channel buffered amount changed:${dc.bufferedAmount}")
        }
    }

    override fun destroy() {
        channel?.close()
        pc?.close()
    }
}

