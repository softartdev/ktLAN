package com.softartdev.ktlan.data.webrtc

import android.content.Context
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

/**
 * This class handles all around WebRTC peer connections.
 */
class AndroidClientWebRTC(
    context: Context,
) : ServerlessRTCClient() {
    lateinit var pc: PeerConnection
    private var pcInitialized: Boolean = false
    var channel: DataChannel? = null

    /**
     * List of servers that will be used to establish the direct connection, STUN/TURN should be supported.
     */
    val iceServers = arrayListOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    val pcConstraints = object : MediaConstraints() {
        init {
            optional.add(KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
    }
    val pcf: PeerConnectionFactory by lazy {
        val initializeOptions = PeerConnectionFactory.InitializationOptions
            .builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializeOptions)
        val options = PeerConnectionFactory.Options()
        return@lazy PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
    }

    /**
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    override fun processOffer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.CREATING_ANSWER
        if (type != null && sdp != null && type == "offer") {
            val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
            pcInitialized = true
            pc = pcf.createPeerConnection(
                iceServers,
                object : DefaultObserver(console, channel) {
                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                        p0?.forEach { console.d("ice candidates removed: {${it.serverUrl}") }
                    }

                    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                        console.d("onAddTrack")
                    }

                    override fun onIceCandidate(p0: IceCandidate?) {
                        console.d("ice candidate:{${p0?.sdp}}")
                    }

                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                        super.onIceGatheringChange(p0)
                        if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
                            console.printf("Here is your answer:")
                            val sdpJSON: String? = serializeSdp(
                                type = pc.localDescription.type.canonicalForm(),
                                sdp = pc.localDescription.description
                            )
                            sdpJSON?.let(console::greenf)
                            p2pState = P2pState.WAITING_TO_CONNECT
                        }
                    }

                    override fun onDataChannel(p0: DataChannel?) {
                        super.onDataChannel(p0)
                        requireNotNull(p0)
                        channel = p0
                        val observer = DefaultDataChannelObserver(p0, this@AndroidClientWebRTC, pc)
                        p0.registerObserver(observer)
                    }
                })!!
            //we have remote offer, let's create answer for that
            pc.setRemoteDescription(object : DefaultSdpObserver(console) {
                override fun onSetSuccess() {
                    super.onSetSuccess()
                    console.d("Remote description set.")
                    pc.createAnswer(object : DefaultSdpObserver(console) {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            //answer is ready, set it
                            console.d("Local description set.")
                            pc.setLocalDescription(DefaultSdpObserver(console), p0)
                        }
                    }, pcConstraints)
                }
            }, offer)
        } else {
            console.redf("Invalid or unsupported offer.")
            p2pState = P2pState.WAITING_FOR_OFFER
        }
    }

    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    override fun processAnswer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        p2pState = P2pState.WAITING_TO_CONNECT
        if (type != null && sdp != null && type == "answer") {
            val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            pc.setRemoteDescription(DefaultSdpObserver(console), answer)
        } else {
            console.redf("Invalid or unsupported answer.")
            p2pState = P2pState.WAITING_FOR_ANSWER
        }
    }

    /**
     * App creates the offer.
     */
    override fun makeOffer() {
        p2pState = P2pState.CREATING_OFFER
        pcInitialized = true
        pc = pcf.createPeerConnection(
            iceServers,
            object : DefaultObserver(console, channel) {
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                    console.d("ice candidates removed: {${p0?.joinToString()}}")
                }

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                    console.d("onAddTrack, p0: $p0, p1: ${p1?.joinToString()}")
                }

                override fun onIceCandidate(p0: IceCandidate?) {
                    console.d("ice candidate:{${p0?.sdp}}")
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    super.onIceGatheringChange(p0)
                    if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
                        console.printf("Your offer is:")
                        val sdpJSON: String? = serializeSdp(
                            type = pc.localDescription.type.canonicalForm(),
                            sdp = pc.localDescription.description
                        )
                        sdpJSON?.let(console::greenf)
                        p2pState = P2pState.WAITING_FOR_ANSWER
                    }
                }
            })!!
        makeDataChannel()
        pc.createOffer(object : DefaultSdpObserver(console) {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    console.d("offer updated")
                    pc.setLocalDescription(object : DefaultSdpObserver(console) {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                    }, p0)
                }
            }
        }, pcConstraints)
    }

    /**
     * Sends message to other party.
     */
    override fun sendMessage(message: String) {
        if (channel == null || p2pState == P2pState.CHAT_ESTABLISHED) {
            val json: String = serializeMessage(message)
            val buf = ByteBuffer.wrap(json.toByteArray(Charsets.UTF_8))
            channel?.send(DataChannel.Buffer(buf, false))
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    /**
     * Creates data channel for use when offer is created on this machine.
     */
    override fun makeDataChannel() {
        val init = DataChannel.Init()
        channel = pc.createDataChannel("test", init)
        val observer = DefaultDataChannelObserver(channel!!, this@AndroidClientWebRTC, pc)
        channel!!.registerObserver(observer)
    }

    /**
     * Clean up some resources.
     */
    override fun destroy() {
        channel?.close()
        if (pcInitialized) {
            pc.close()
        }
    }
}