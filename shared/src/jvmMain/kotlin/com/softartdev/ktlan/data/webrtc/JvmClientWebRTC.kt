package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.RTCAnswerOptions
import dev.onvoid.webrtc.RTCConfiguration
import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.RTCDataChannelBuffer
import dev.onvoid.webrtc.RTCDataChannelInit
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.RTCIceGatheringState
import dev.onvoid.webrtc.RTCIceServer
import dev.onvoid.webrtc.RTCOfferOptions
import dev.onvoid.webrtc.RTCPeerConnection
import dev.onvoid.webrtc.RTCSdpType
import dev.onvoid.webrtc.RTCSessionDescription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.ByteBuffer

/**
 * WebRTC client implementation for desktop JVM platform.
 */
class JvmClientWebRTC : ServerlessRTCClient() {

    private lateinit var factory: PeerConnectionFactory
    private lateinit var pc: RTCPeerConnection
    private var pcInitialized: Boolean = false

    private var channel: RTCDataChannel? = null

    /** STUN server list used to establish a direct connection. */
    private val iceServers: List<RTCIceServer> = listOf(
        RTCIceServer().apply { urls = listOf("stun:stun.l.google.com:19302") }
    )

    private val json = Json { ignoreUnknownKeys = true }

    override fun init() {
        factory = PeerConnectionFactory()
        p2pState = P2pState.INITIALIZING
    }

    /**
     * Converts session description object to JSON object that can be used in other applications.
     * This is what is passed between parties to maintain connection. We need to pass the session description to the other side.
     * In normal use case we should use some kind of signalling server, but for this demo you can use some other method to pass it there (like e-mail).
     */
    private fun sessionDescriptionToJSON(sessDesc: RTCSessionDescription): String =
        buildJsonObject {
            put(JSON_TYPE, sessDesc.sdpType.name.lowercase())
            put(JSON_SDP, sessDesc.sdp)
        }.toString()

    override fun processOffer(sdpJSON: String) {
        try {
            val obj: JsonObject = json.parseToJsonElement(sdpJSON).jsonObject
            val type = obj[JSON_TYPE]?.jsonPrimitive?.content
            val sdp = obj[JSON_SDP]?.jsonPrimitive?.content
            p2pState = P2pState.CREATING_ANSWER
            if (type == "offer" && sdp != null) {
                pcInitialized = true
                val config = RTCConfiguration().apply {
                    iceServers.addAll(this@JvmClientWebRTC.iceServers)
                }
                pc = factory.createPeerConnection(
                    config,
                    object : DefaultObserver(console, channel) {
                        override fun onIceCandidate(candidate: RTCIceCandidate?) {
                            console.d("ice candidate:{${candidate?.sdp}}")
                        }

                        override fun onIceGatheringChange(state: RTCIceGatheringState?) {
                            super.onIceGatheringChange(state)
                            if (state == RTCIceGatheringState.COMPLETE) {
                                console.printf("Your answer is:")
                                console.greenf(sessionDescriptionToJSON(pc.localDescription))
                                p2pState = P2pState.WAITING_FOR_ANSWER
                            }
                        }

                        override fun onDataChannel(dataChannel: RTCDataChannel?) {
                            super.onDataChannel(dataChannel)
                            if (dataChannel != null) {
                                console.d("data channel ${dataChannel.label} established")
                                channel = dataChannel
                                makeDataChannel()
                                p2pState = P2pState.CHAT_ESTABLISHED
                            }
                        }
                    })!!

                val offer = RTCSessionDescription(RTCSdpType.OFFER, sdp)
                pc.setRemoteDescription(offer, DefaultSetDescObserver(console))

                pc.createAnswer(RTCAnswerOptions(), DefaultCreateDescObserver(console) { answer ->
                    pc.setLocalDescription(answer, DefaultSetDescObserver(console))
                })
            } else {
                console.redf("Invalid or unsupported offer.")
                p2pState = P2pState.WAITING_FOR_OFFER
            }
        } catch (e: Exception) {
            console.redf("bad json")
            p2pState = P2pState.WAITING_FOR_OFFER
        }
    }

    override fun processAnswer(sdpJSON: String) {
        try {
            val obj = json.parseToJsonElement(sdpJSON).jsonObject
            val type = obj[JSON_TYPE]?.jsonPrimitive?.content
            val sdp = obj[JSON_SDP]?.jsonPrimitive?.content
            p2pState = P2pState.WAITING_TO_CONNECT
            if (type == "answer" && sdp != null) {
                val answer = RTCSessionDescription(RTCSdpType.ANSWER, sdp)
                pc.setRemoteDescription(answer, DefaultSetDescObserver(console))
            } else {
                console.redf("Invalid or unsupported answer.")
                p2pState = P2pState.WAITING_FOR_ANSWER
            }
        } catch (e: Exception) {
            console.redf("bad json")
            p2pState = P2pState.WAITING_FOR_ANSWER
        }
    }

    override fun makeOffer() {
        p2pState = P2pState.CREATING_OFFER
        pcInitialized = true
        val config = RTCConfiguration().apply {
            iceServers.addAll(this@JvmClientWebRTC.iceServers)
        }
        pc = factory.createPeerConnection(config, object : DefaultObserver(console, channel) {
            override fun onIceCandidate(candidate: RTCIceCandidate?) {
                console.d("ice candidate:{${candidate?.sdp}}")
            }

            override fun onIceGatheringChange(state: RTCIceGatheringState?) {
                super.onIceGatheringChange(state)
                if (state == RTCIceGatheringState.COMPLETE) {
                    console.printf("Your offer is:")
                    console.greenf(sessionDescriptionToJSON(pc.localDescription))
                    p2pState = P2pState.WAITING_FOR_ANSWER
                }
            }
        })!!
        makeDataChannel()
        pc.createOffer(RTCOfferOptions(), DefaultCreateDescObserver(console) { offer ->
            pc.setLocalDescription(offer, DefaultSetDescObserver(console))
            console.printf("Your offer is:")
            console.greenf(sessionDescriptionToJSON(offer))
            p2pState = P2pState.WAITING_FOR_ANSWER
        })
    }

    override fun sendMessage(message: String) {
        val ch = channel
        if (ch != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val text = buildJsonObject { put(JSON_MESSAGE, message) }.toString()
            val buf = ByteBuffer.wrap(text.toByteArray(Charsets.UTF_8))
            ch.send(RTCDataChannelBuffer(buf, false))
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    override fun makeDataChannel() {
        val init = RTCDataChannelInit()
        channel = pc.createDataChannel("test", init)
        channel!!.registerObserver(
            DefaultDataChannelObserver(
                channel = channel!!,
                charset = Charsets.UTF_8,
                console = console,
                setState = ::p2pState::set,
                jsonMessage = JSON_MESSAGE,
                pc = pc
            )
        )
    }

    override fun destroy() {
        channel?.close()
        if (pcInitialized) {
            pc.close()
        }
    }
}
