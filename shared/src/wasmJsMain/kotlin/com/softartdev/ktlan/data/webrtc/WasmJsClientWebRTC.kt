package com.softartdev.ktlan.data.webrtc

import kotlin.js.Promise
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.js.JsAny
import kotlin.js.JsArray

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

    private fun sessionDescriptionToJSON(desc: RTCSessionDescription): String =
        Json.encodeToString(
            buildJsonObject {
                put(JSON_TYPE, desc.type)
                put(JSON_SDP, desc.sdp)
            }
        )

    override fun processOffer(sdpJSON: String) {
        try {
            val jsonObj = Json.parseToJsonElement(sdpJSON).jsonObject
            val type = jsonObj[JSON_TYPE]?.jsonPrimitive?.content
            val sdp = jsonObj[JSON_SDP]?.jsonPrimitive?.content
            p2pState = P2pState.CREATING_ANSWER
            if (type == "offer" && sdp != null) {
                val peer = RTCPeerConnection(defaultRtcConfig())
                pc = peer
                peer.onicecandidate = { event ->
                    console.d("ice candidate:{${event.candidate?.candidate}}")
                }
                peer.onicegatheringstatechange = {
                    if (peer.iceGatheringState == "complete") {
                        console.printf("Here is your answer:")
                        peer.localDescription?.let { console.greenf(sessionDescriptionToJSON(it)) }
                        p2pState = P2pState.WAITING_TO_CONNECT
                    }
                }
                peer.ondatachannel = { evt ->
                    val dc = evt.channel
                    channel = dc
                    registerDataChannel(dc)
                }
                peer.setRemoteDescription(createSdp(type, sdp)).then {
                    peer.createAnswer().then { answer ->
                        peer.setLocalDescription(answer)
                    }
                }
            } else {
                console.redf("Invalid or unsupported offer.")
                p2pState = P2pState.WAITING_FOR_OFFER
            }
        } catch (t: Throwable) {
            console.redf("bad json")
            p2pState = P2pState.WAITING_FOR_OFFER
        }
    }

    override fun processAnswer(sdpJSON: String) {
        try {
            val jsonObj = Json.parseToJsonElement(sdpJSON).jsonObject
            val type = jsonObj[JSON_TYPE]?.jsonPrimitive?.content
            val sdp = jsonObj[JSON_SDP]?.jsonPrimitive?.content
            p2pState = P2pState.WAITING_TO_CONNECT
            val peer = pc
            if (peer != null && type == "answer" && sdp != null) {
                peer.setRemoteDescription(createSdp(type, sdp))
            } else {
                console.redf("Invalid or unsupported answer.")
                p2pState = P2pState.WAITING_FOR_ANSWER
            }
        } catch (t: Throwable) {
            console.redf("bad json")
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
                peer.localDescription?.let { console.greenf(sessionDescriptionToJSON(it)) }
                p2pState = P2pState.WAITING_FOR_ANSWER
            }
        }
        makeDataChannel()
        peer.createOffer().then { offer ->
            peer.setLocalDescription(offer)
        }
    }

    override fun sendMessage(message: String) {
        val dc = channel
        if (dc != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val data = Json.encodeToString(buildJsonObject { put(JSON_MESSAGE, message) })
            dc.send(data)
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    override fun makeDataChannel() {
        val peer = pc ?: return
        val dc = peer.createDataChannel("test", emptyDataChannelInit())
        channel = dc
        registerDataChannel(dc)
    }

    private fun registerDataChannel(dc: RTCDataChannel) {
        dc.onmessage = { event ->
            val dataStr = event.data?.toString() ?: ""
            val msg = try {
                Json.parseToJsonElement(dataStr).jsonObject[JSON_MESSAGE]?.jsonPrimitive?.content
            } catch (_: Throwable) {
                null
            }
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

