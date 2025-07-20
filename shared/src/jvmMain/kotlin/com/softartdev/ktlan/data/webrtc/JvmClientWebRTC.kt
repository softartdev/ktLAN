package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

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

    private val UTF_8: Charset = Charsets.UTF_8

    override fun init() {
        factory = PeerConnectionFactory()
        p2pState = P2pState.INITIALIZING
    }

    override fun processOffer(sdpJSON: String) {
        val type = Regex("""\"$JSON_TYPE\"\s*:\s*\"(.*?)\"""").find(sdpJSON)?.groupValues?.getOrNull(1)
        val sdp = Regex("""\"$JSON_SDP\"\s*:\s*\"(.*?)\"""").find(sdpJSON)?.groupValues?.getOrNull(1)
        if (type == "offer" && sdp != null) {
            p2pState = P2pState.CREATING_ANSWER
            pcInitialized = true
            val config = RTCConfiguration().apply {
                iceServers.addAll(this@JvmClientWebRTC.iceServers)
            }
            pc = factory.createPeerConnection(config, DefaultObserver(console, channel))!!

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
        val type = Regex("""\"$JSON_TYPE\"\s*:\s*\"(.*?)\"""").find(sdpJSON)?.groupValues?.getOrNull(1)
        val sdp = Regex("""\"$JSON_SDP\"\s*:\s*\"(.*?)\"""").find(sdpJSON)?.groupValues?.getOrNull(1)
        if (type == "answer" && sdp != null) {
            p2pState = P2pState.WAITING_TO_CONNECT
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
        pc = factory.createPeerConnection(config, DefaultObserver(console, channel))!!
        makeDataChannel()
        pc.createOffer(RTCOfferOptions(), DefaultCreateDescObserver(console) { offer ->
            pc.setLocalDescription(offer, DefaultSetDescObserver(console))
            console.printf("Your offer is:")
            console.greenf("{\"$JSON_TYPE\":\"offer\",\"$JSON_SDP\":\"${offer.sdp}\"}")
            p2pState = P2pState.WAITING_FOR_ANSWER
        })
    }

    override fun sendMessage(message: String) {
        val ch = channel
        if (ch != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val json = "{\"$JSON_MESSAGE\":\"$message\"}"
            val buf = ByteBuffer.wrap(json.toByteArray(UTF_8))
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
                UTF_8 = UTF_8,
                console = console,
                setState = ::p2pState::set,
                JSON_MESSAGE = JSON_MESSAGE,
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
