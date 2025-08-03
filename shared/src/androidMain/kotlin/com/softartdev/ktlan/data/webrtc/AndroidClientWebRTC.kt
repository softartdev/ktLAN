package com.softartdev.ktlan.data.webrtc

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
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
    lateinit var pcf: PeerConnectionFactory

    init {
        val initializeOptions = PeerConnectionFactory.InitializationOptions
            .builder(context)
            /*.setEnableVideoHwAcceleration(false)*/
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializeOptions)
        val options = PeerConnectionFactory.Options()
        pcf = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
    }

    /**
     * Converts session description object to JSON object that can be used in other applications.
     * This is what is passed between parties to maintain connection. We need to pass the session description to the other side.
     * In normal use case we should use some kind of signalling server, but for this demo you can use some other method to pass it there (like e-mail).
     */
    private fun sessionDescriptionToJSON(sessDesc: SessionDescription): JSONObject {
        val json = JSONObject()
        json.put(JSON_TYPE, sessDesc.type.canonicalForm())
        json.put(JSON_SDP, sessDesc.description)
        return json
    }

    /**
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    override fun processOffer(sdpJSON: String) {
        try {
            val json = JSONObject(sdpJSON)
            val type = json.getString(JSON_TYPE)
            val sdp = json.getString(JSON_SDP)
            p2pState = P2pState.CREATING_ANSWER
            if (type != null && sdp != null && type == "offer") {
                val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                pcInitialized = true
                pc = pcf.createPeerConnection(
                    iceServers,
                    pcConstraints,
                    object : DefaultObserver(console, channel) {
                        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                            p0?.forEach { console.d("ice candidatesremoved: {${it.serverUrl}") }
                        }

                        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                            console.d("onAddTrack")
                        }

                        override fun onIceCandidate(p0: IceCandidate?) {
                            console.d("ice candidate:{${p0?.sdp}}")
                        }

                        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                            super.onIceGatheringChange(p0)
                            //ICE gathering complete, we should have answer now
                            if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
                                console.printf("Here is your answer:")
                                console.greenf("${sessionDescriptionToJSON(pc.localDescription)}")
                                p2pState = P2pState.WAITING_TO_CONNECT
                            }
                        }

                        override fun onDataChannel(p0: DataChannel?) {
                            super.onDataChannel(p0)
                            channel = p0
                            p0?.registerObserver(
                                DefaultDataChannelObserver(
                                    channel = p0,
                                    charset = Charsets.UTF_8,
                                    console = console,
                                    setState = ::p2pState::set,
                                    jsonMessage = JSON_MESSAGE,
                                    pc = pc
                                )
                            )
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
        } catch (e: JSONException) {
            console.redf("bad json")
            p2pState = P2pState.WAITING_FOR_OFFER
        }
    }

    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    override fun processAnswer(sdpJSON: String) {
        try {
            val json = JSONObject(sdpJSON)
            val type = json.getString(JSON_TYPE)
            val sdp = json.getString(JSON_SDP)
            p2pState = P2pState.WAITING_TO_CONNECT
            if (type != null && sdp != null && type == "answer") {
                val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                pc.setRemoteDescription(DefaultSdpObserver(console), answer)
            } else {
                console.redf("Invalid or unsupported answer.")
                p2pState = P2pState.WAITING_FOR_ANSWER
            }
        } catch (e: JSONException) {
            console.redf("bad json")
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
            pcConstraints,
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
                        console.greenf("${sessionDescriptionToJSON(pc.localDescription)}")
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
            val sendJSON = JSONObject()
            sendJSON.put(JSON_MESSAGE, message)
            val buf = ByteBuffer.wrap(sendJSON.toString().toByteArray(Charsets.UTF_8))
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