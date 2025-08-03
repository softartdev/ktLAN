@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.softartdev.ktlan.data.webrtc

import cocoapods.WebRTC.*
import cocoapods.WebRTC.RTCIceServer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSData
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSJSONWritingOptions
import platform.Foundation.NSJSONWritingPrettyPrinted
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

class IOSClientWebRTC : ServerlessRTCClient() {
    private var factory: RTCPeerConnectionFactory = RTCPeerConnectionFactory()
    private lateinit var pc: RTCPeerConnection
    private var channel: RTCDataChannel? = null
    private var pcInitialized: Boolean = false

    private val iceServers: List<RTCIceServer> = listOf(
        RTCIceServer(listOf("stun:stun.l.google.com:19302"))
    )

    private val constraints = RTCMediaConstraints(
        mandatoryConstraints = null,
        optionalConstraints = mapOf("DtlsSrtpKeyAgreement" to "true")
    )

    private fun sessionDescriptionToJSON(sessDesc: RTCSessionDescription): String {
        val typeString = when (sessDesc.type) {
            RTCSdpType.RTCSdpTypeOffer -> "offer"
            RTCSdpType.RTCSdpTypeAnswer -> "answer"
            else -> sessDesc.type.name
        }
        val map: Map<String, Any?> = mapOf(
            JSON_TYPE to typeString,
            JSON_SDP to sessDesc.sdp
        )
        val options: NSJSONWritingOptions = NSJSONWritingPrettyPrinted
        val data: NSData = NSJSONSerialization.dataWithJSONObject(map, options, null)!!
        return NSString.create(data, NSUTF8StringEncoding).toString()
    }

    override fun processOffer(sdpJSON: String) {
        try {
            val nsData: NSData = NSString.create(sdpJSON)!!.dataUsingEncoding(NSUTF8StringEncoding)!!
            val obj: Map<*, *> = NSJSONSerialization.JSONObjectWithData(
                data = nsData,
                options = NSJSONWritingPrettyPrinted,
                error = null
            ) as Map<*, *>
            val type = obj[JSON_TYPE] as? String
            val sdp = obj[JSON_SDP] as? String
            p2pState = P2pState.CREATING_ANSWER
            if (type == "offer" && sdp != null) {
                pcInitialized = true
                val config = RTCConfiguration()
                config.iceServers = iceServers
                pc = factory.peerConnectionWithConfiguration(
                    configuration = config,
                    constraints = constraints,
                    delegate = object : NSObject(), RTCPeerConnectionDelegateProtocol {

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didChangeIceGatheringState: RTCIceGatheringState
                        ) {
                            if (didChangeIceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) {
                                console.printf("Here is your answer:")
                                console.greenf(sessionDescriptionToJSON(pc.localDescription!!))
                                p2pState = P2pState.WAITING_TO_CONNECT
                            }
                        }

                        @ObjCSignatureOverride
                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didRemoveStream: RTCMediaStream
                        ) {
                            console.d("Stream removed")
                        }

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didOpenDataChannel: RTCDataChannel
                        ) {
                            channel = didOpenDataChannel
                            channel?.delegate =
                                object : NSObject(), RTCDataChannelDelegateProtocol {
                                    override fun dataChannel(
                                        dataChannel: RTCDataChannel,
                                        didReceiveMessageWithBuffer: RTCDataBuffer
                                    ) {
                                        val nsData: NSData = didReceiveMessageWithBuffer.data
                                        val obj = NSJSONSerialization.JSONObjectWithData(
                                            data = nsData,
                                            options = NSJSONWritingPrettyPrinted,
                                            error = null
                                        ) as Map<*, *>
                                        val message = obj[JSON_MESSAGE] as? String
                                        if (message != null) {
                                            console.bluef(">$message")
                                        } else {
                                            console.redf("Malformed message received")
                                        }
                                    }

                                    override fun dataChannelDidChangeState(dataChannel: RTCDataChannel) {
                                        val state = dataChannel.readyState
                                        console.d("Channel state changed:$state")
                                        if (state == RTCDataChannelState.RTCDataChannelStateOpen) {
                                            p2pState = P2pState.CHAT_ESTABLISHED
                                            console.bluef("Chat established.")
                                            val remoteAddress =
                                                pc.remoteDescription?.sdp ?: "unknown"
                                            console.printf("Connected to remote peer: $remoteAddress")
                                        } else if (state == RTCDataChannelState.RTCDataChannelStateClosed) {
                                            p2pState = P2pState.CHAT_ENDED
                                            console.redf("Chat ended.")
                                        }
                                    }
                                }
                        }

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didRemoveIceCandidates: List<*>
                        ) {
                            for (candidate in didRemoveIceCandidates) {
                                if (candidate is RTCIceCandidate) {
                                    console.d("ice candidate removed: {${candidate.sdp}}")
                                }
                            }
                        }

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didChangeIceConnectionState: RTCIceConnectionState
                        ) {
                            console.d("ICE connection state changed: ${didChangeIceConnectionState.name}")
                        }

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didGenerateIceCandidate: RTCIceCandidate
                        ) {
                            console.d("ice candidate: {${didGenerateIceCandidate.sdp}}")
                        }

                        override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {
                            console.d("Peer connection should negotiate")
                        }

                        @ObjCSignatureOverride
                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didAddStream: RTCMediaStream
                        ) {
                            console.d("Stream added")
                        }

                        override fun peerConnection(
                            peerConnection: RTCPeerConnection,
                            didChangeSignalingState: RTCSignalingState
                        ) {
                            console.d("Signaling state changed: ${didChangeSignalingState.name}")
                        }
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
            val nsData: NSData = NSString.create(sdpJSON)!!.dataUsingEncoding(NSUTF8StringEncoding)!!
            val obj: Map<*, *> = NSJSONSerialization.JSONObjectWithData(
                data = nsData,
                options = NSJSONWritingPrettyPrinted,
                error = null
            ) as Map<*, *>
            val type = obj[JSON_TYPE] as? String
            val sdp = obj[JSON_SDP] as? String
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
        val config = RTCConfiguration()
        config.iceServers = iceServers
        pc = factory.peerConnectionWithConfiguration(
            configuration = config,
            constraints = constraints,
            delegate = object : NSObject(), RTCPeerConnectionDelegateProtocol {

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didChangeIceGatheringState: RTCIceGatheringState
                ) {
                    if (didChangeIceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) {
                        console.printf("Your offer is:")
                        console.greenf(sessionDescriptionToJSON(pc.localDescription!!))
                        p2pState = P2pState.WAITING_FOR_ANSWER
                    }
                }

                @ObjCSignatureOverride
                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didRemoveStream: RTCMediaStream
                ) {
                    console.d("Stream removed")
                }

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didOpenDataChannel: RTCDataChannel
                ) {
                    channel = didOpenDataChannel
                    channel?.delegate = object : NSObject(), RTCDataChannelDelegateProtocol {
                        override fun dataChannel(
                            dataChannel: RTCDataChannel,
                            didReceiveMessageWithBuffer: RTCDataBuffer
                        ) {
                            val nsData = didReceiveMessageWithBuffer.data
                            val obj = NSJSONSerialization.JSONObjectWithData(
                                data = nsData,
                                options = NSJSONWritingPrettyPrinted,
                                error = null
                            ) as Map<*, *>
                            val message = obj[JSON_MESSAGE] as? String
                            if (message != null) {
                                console.bluef(">$message")
                            } else {
                                console.redf("Malformed message received")
                            }
                        }

                        override fun dataChannelDidChangeState(dataChannel: RTCDataChannel) {
                            val state = dataChannel.readyState
                            console.d("Channel state changed:$state")
                            if (state == RTCDataChannelState.RTCDataChannelStateOpen) {
                                p2pState = P2pState.CHAT_ESTABLISHED
                                console.bluef("Chat established.")
                                val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
                                console.printf("Connected to remote peer: $remoteAddress")
                            } else if (state == RTCDataChannelState.RTCDataChannelStateClosed) {
                                p2pState = P2pState.CHAT_ENDED
                                console.redf("Chat ended.")
                            }
                        }
                    }
                }

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didRemoveIceCandidates: List<*>
                ) {
                    for (candidate in didRemoveIceCandidates) {
                        if (candidate is RTCIceCandidate) {
                            console.d("ice candidate removed: {${candidate.sdp}}")
                        }
                    }
                }

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didChangeIceConnectionState: RTCIceConnectionState
                ) {
                    console.d("ICE connection state changed: ${didChangeIceConnectionState.name}")
                }

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didGenerateIceCandidate: RTCIceCandidate
                ) {
                    console.d("ice candidate: {${didGenerateIceCandidate.sdp}}")
                }

                override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {
                    console.d("Peer connection should negotiate")
                }

                @ObjCSignatureOverride
                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didAddStream: RTCMediaStream
                ) {
                    console.d("Stream added")
                }

                override fun peerConnection(
                    peerConnection: RTCPeerConnection,
                    didChangeSignalingState: RTCSignalingState
                ) {
                    console.d("Signaling state changed: ${didChangeSignalingState.name}")
                }

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
            val text = "{\"$JSON_MESSAGE\":\"$message\"}"
            val nSString = NSString.create(text)
            val nsData = nSString!!.dataUsingEncoding(NSUTF8StringEncoding)!!
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
            override fun dataChannel(
                dataChannel: RTCDataChannel,
                didReceiveMessageWithBuffer: RTCDataBuffer
            ) {
                val nsData = didReceiveMessageWithBuffer.data
                val obj: Map<*, *> = NSJSONSerialization.JSONObjectWithData(
                    data = nsData,
                    options = NSJSONWritingPrettyPrinted,
                    error = null
                ) as Map<*, *>
                val message = obj[JSON_MESSAGE] as? String
                if (message != null) {
                    console.bluef(">$message")
                } else {
                    console.redf("Malformed message received")
                }
            }

            override fun dataChannelDidChangeState(dataChannel: RTCDataChannel) {
                val state = dataChannel.readyState
                console.d("Channel state changed:$state")
                if (state == RTCDataChannelState.RTCDataChannelStateOpen) {
                    p2pState = P2pState.CHAT_ESTABLISHED
                    console.bluef("Chat established.")
                    val remoteAddress = pc.remoteDescription?.sdp ?: "unknown"
                    console.printf("Connected to remote peer: $remoteAddress")
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
