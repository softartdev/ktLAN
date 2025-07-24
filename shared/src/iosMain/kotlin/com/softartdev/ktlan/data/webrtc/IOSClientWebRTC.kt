package com.softartdev.ktlan.data.webrtc

import cocoapods.GoogleWebRTC.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.native.concurrent.freeze
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSUTF8StringEncoding
import platform.darwin.NSObject

class IOSClientWebRTC : ServerlessRTCClient() {
    private lateinit var factory: RTCPeerConnectionFactory
    private lateinit var pc: RTCPeerConnection
    private var channel: RTCDataChannel? = null
    private var pcInitialized: Boolean = false

    private val iceServers: List<RTCIceServer> = listOf(
        RTCIceServer.alloc()!!.initWithURLStrings(listOf("stun:stun.l.google.com:19302"))
    )

    private val constraints = RTCMediaConstraints(
        mandatoryConstraints = null,
        optionalConstraints = mapOf("DtlsSrtpKeyAgreement" to "true")
    )

    private val json = Json { ignoreUnknownKeys = true }

    override fun init() {
        factory = RTCPeerConnectionFactory()
        p2pState = P2pState.INITIALIZING
    }

    private fun sessionDescriptionToJSON(sessDesc: RTCSessionDescription): String =
        buildJsonObject {
            val typeString = when (sessDesc.type) {
                RTCSdpType.RTCSdpTypeOffer -> "offer"
                RTCSdpType.RTCSdpTypeAnswer -> "answer"
                else -> sessDesc.type.name
            }
            put(JSON_TYPE, typeString)
            put(JSON_SDP, sessDesc.sdp ?: "")
        }.toString()

    override fun processOffer(sdpJSON: String) {
        try {
            val obj = json.parseToJsonElement(sdpJSON).jsonObject
            val type = obj[JSON_TYPE]?.jsonPrimitive?.content
            val sdp = obj[JSON_SDP]?.jsonPrimitive?.content
            p2pState = P2pState.CREATING_ANSWER
            if (type == "offer" && sdp != null) {
                pcInitialized = true
                val config = RTCConfiguration(iceServers = iceServers)
                pc = factory.peerConnectionWithConfiguration(configuration = config, constraints = constraints, delegate = object : NSObject(), RTCPeerConnectionDelegateProtocol {
                    override fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceGatheringState: RTCIceGatheringState) {
                        if (didChangeIceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) {
                            console.printf("Here is your answer:")
                            console.greenf(sessionDescriptionToJSON(pc.localDescription!!))
                            p2pState = P2pState.WAITING_TO_CONNECT
                        }
                    }

                    override fun peerConnection(peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
                        console.d("ice candidate:{${'$'}{candidate.sdp}}")
                    }

                    override fun peerConnection(peerConnection: RTCPeerConnection, didRemove candidates: List<*>) {
                        console.d("ice candidates removed")
                    }

                    override fun peerConnection(peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {
                        channel = dataChannel
                        channel?.delegate = object : NSObject(), RTCDataChannelDelegateProtocol {
                            override fun dataChannel(dataChannel: RTCDataChannel, didReceiveMessageWithBuffer: RTCDataBuffer) {
                                val nsData = didReceiveMessageWithBuffer.data
                                val text = NSString.create(nsData, NSUTF8StringEncoding) as String
                                val msgObj = json.parseToJsonElement(text).jsonObject
                                val message = msgObj[JSON_MESSAGE]?.jsonPrimitive?.content
                                if (message != null) {
                                    console.bluef(">${'$'}message")
                                } else {
                                    console.redf("Malformed message received")
                                }
                            }

                            override fun dataChannelDidChangeState(dataChannel: RTCDataChannel) {
                                val state = dataChannel.readyState
                                console.d("Channel state changed:${'$'}state")
                                if (state == RTCDataChannelState.RTCDataChannelStateOpen) {
                                    p2pState = P2pState.CHAT_ESTABLISHED
                                    console.bluef("Chat established.")
                                    val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
                                    console.printf("Connected to remote peer: ${'$'}remoteAddress")
                                } else if (state == RTCDataChannelState.RTCDataChannelStateClosed) {
                                    p2pState = P2pState.CHAT_ENDED
                                    console.redf("Chat ended.")
                                }
                            }
                        }
                    }

                    // other delegate methods we don't use
                    override fun peerConnection(peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {}
                    override fun peerConnection(peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {}
                    override fun peerConnection(peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
                    override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {}
                    override fun peerConnection(peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {}
                })!!

                val offer = RTCSessionDescription(type = RTCSdpType.RTCSdpTypeOffer, sdp = sdp)
                pc.setRemoteDescription(offer) { error ->
                    if (error == null) {
                        pc.answerForConstraints(constraints) { answer, err ->
                            if (answer != null) {
                                pc.setLocalDescription(answer) {}
                            }
                        }
                    }
                }
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
                val answer = RTCSessionDescription(type = RTCSdpType.RTCSdpTypeAnswer, sdp = sdp)
                pc.setRemoteDescription(answer) {}
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
        val config = RTCConfiguration(iceServers = iceServers)
        pc = factory.peerConnectionWithConfiguration(configuration = config, constraints = constraints, delegate = object : NSObject(), RTCPeerConnectionDelegateProtocol {
            override fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceGatheringState: RTCIceGatheringState) {
                if (didChangeIceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) {
                    console.printf("Your offer is:")
                    console.greenf(sessionDescriptionToJSON(pc.localDescription!!))
                    p2pState = P2pState.WAITING_FOR_ANSWER
                }
            }
            override fun peerConnection(peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
                console.d("ice candidate:{${'$'}{candidate.sdp}}")
            }
            override fun peerConnection(peerConnection: RTCPeerConnection, didRemove candidates: List<*>) {
                console.d("ice candidates removed")
            }
            override fun peerConnection(peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {
                channel = dataChannel
                makeDataChannel()
            }
            override fun peerConnection(peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {}
            override fun peerConnection(peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {}
            override fun peerConnection(peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
            override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {}
            override fun peerConnection(peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {}
        })!!
        makeDataChannel()
        pc.offerForConstraints(constraints) { offer, error ->
            if (offer != null) {
                pc.setLocalDescription(offer) {}
            }
        }
    }

    override fun sendMessage(message: String) {
        val ch = channel
        if (ch != null && p2pState == P2pState.CHAT_ESTABLISHED) {
            val text = buildJsonObject { put(JSON_MESSAGE, message) }.toString()
            val nsData = (text as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
            val buffer = RTCDataBuffer(data = nsData, isBinary = false)
            ch.sendData(buffer)
        } else {
            console.redf("Error. Chat is not established.")
        }
    }

    override fun makeDataChannel() {
        val init = RTCDataChannelConfiguration()
        channel = pc.dataChannelForLabel("test", init)
        channel?.delegate = object : NSObject(), RTCDataChannelDelegateProtocol {
            override fun dataChannel(dataChannel: RTCDataChannel, didReceiveMessageWithBuffer: RTCDataBuffer) {
                val nsData = didReceiveMessageWithBuffer.data
                val text = NSString.create(nsData, NSUTF8StringEncoding) as String
                val obj = json.parseToJsonElement(text).jsonObject
                val message = obj[JSON_MESSAGE]?.jsonPrimitive?.content
                if (message != null) {
                    console.bluef(">${'$'}message")
                } else {
                    console.redf("Malformed message received")
                }
            }

            override fun dataChannelDidChangeState(dataChannel: RTCDataChannel) {
                val state = dataChannel.readyState
                console.d("Channel state changed:${'$'}state")
                if (state == RTCDataChannelState.RTCDataChannelStateOpen) {
                    p2pState = P2pState.CHAT_ESTABLISHED
                    console.bluef("Chat established.")
                    val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
                    console.printf("Connected to remote peer: ${'$'}remoteAddress")
                } else if (state == RTCDataChannelState.RTCDataChannelStateClosed) {
                    p2pState = P2pState.CHAT_ENDED
                    console.redf("Chat ended.")
                }
            }
        }
    }

    override fun destroy() {
        channel?.close()
        if (pcInitialized) {
            pc.close()
        }
    }
}
