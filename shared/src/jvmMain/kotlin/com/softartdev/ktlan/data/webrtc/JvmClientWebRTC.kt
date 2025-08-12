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
import java.nio.ByteBuffer

/**
 * WebRTC client implementation for desktop JVM platform.
 */
class JvmClientWebRTC : ServerlessRTCClient() {
    private var factory: PeerConnectionFactory = PeerConnectionFactory()
    private lateinit var pc: RTCPeerConnection
    private var pcInitialized: Boolean = false

    private var channel: RTCDataChannel? = null

    /** STUN server list used to establish a direct connection. */
    private val iceServers: List<RTCIceServer> = listOf(
        RTCIceServer().apply { urls = listOf("stun:stun.l.google.com:19302") }
    )

    override fun processOffer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.CREATING_ANSWER
        if (type == "offer" && sdp != null) {
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
                        console.printf("Your answer is:")
                        val sdpJSON: String? = serializeSdp(
                            type = pc.localDescription.sdpType.name.lowercase(),
                            sdp = pc.localDescription.sdp
                        )
                        sdpJSON?.let(console::greenf)
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
    }

    override fun processAnswer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.WAITING_TO_CONNECT
        if (type != null && sdp != null && type == "answer") {
            val answer = RTCSessionDescription(RTCSdpType.ANSWER, sdp)
            pc.setRemoteDescription(answer, DefaultSetDescObserver(console))
        } else {
            console.redf("Invalid or unsupported answer.")
            p2pState = P2pState.WAITING_FOR_ANSWER
        }
    }

    override fun makeOffer() {
        p2pState = P2pState.CREATING_OFFER
        pcInitialized = true
        val config = RTCConfiguration().apply {
            iceServers.addAll(this@JvmClientWebRTC.iceServers)
        }
        pc = factory.createPeerConnection(config, object : DefaultObserver(console, channel){
            override fun onIceCandidate(candidate: RTCIceCandidate?) {
                console.d("ice candidate:{${candidate?.sdp}}")
            }

            override fun onIceGatheringChange(state: RTCIceGatheringState?) {
                super.onIceGatheringChange(state)
                if (state == RTCIceGatheringState.COMPLETE) {
                    console.printf("Your offer is:")
                    val sdpJSON: String? = serializeSdp(
                        type = pc.localDescription.sdpType.name.lowercase(),
                        sdp = pc.localDescription.sdp
                    )
                    sdpJSON?.let(console::greenf)
                    p2pState = P2pState.WAITING_FOR_ANSWER
                }
            }
        })!!
        makeDataChannel()
        pc.createOffer(RTCOfferOptions(), DefaultCreateDescObserver(console) { desc: RTCSessionDescription ->
            pc.setLocalDescription(desc, DefaultSetDescObserver(console))
            console.printf("Your offer is:")
            val sdpJSON: String? = serializeSdp(
                type = desc.sdpType.name.lowercase(),
                sdp = desc.sdp
            )
            sdpJSON?.let(console::greenf)
            p2pState = P2pState.WAITING_FOR_ANSWER
        })
    }

    override fun sendMessage(message: String) {
        val ch = channel
        if (ch != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val json: String = serializeMessage(message)
            val buf = ByteBuffer.wrap(json.toByteArray(Charsets.UTF_8))
            ch.send(RTCDataChannelBuffer(buf, false))
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    override fun makeDataChannel() {
        val init = RTCDataChannelInit()
        channel = pc.createDataChannel("test", init)
        val observer = DefaultDataChannelObserver(channel!!, this@JvmClientWebRTC, pc)
        channel!!.registerObserver(observer)
    }

    override fun destroy() {
        channel?.close()
        if (pcInitialized) {
            pc.close()
        }
    }
}
