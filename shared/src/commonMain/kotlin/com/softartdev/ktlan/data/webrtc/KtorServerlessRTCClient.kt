@file:OptIn(ExperimentalKtorApi::class)

package com.softartdev.ktlan.data.webrtc

import io.ktor.client.webrtc.DataChannelEvent
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcDataChannel
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared serverless WebRTC logic implemented on top of Ktor's WebRTC abstractions.
 */
abstract class KtorServerlessRTCClient(
    private val webRtcClient: WebRtcClient,
) : ServerlessRTCClient() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val operationMutex = Mutex()

    private var peerConnection: WebRtcPeerConnection? = null
    private var dataChannel: WebRtcDataChannel? = null

    private var connectionEventsJob: Job? = null
    private var channelReceiveJob: Job? = null

    override fun processOffer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        if (type?.lowercase() != SDP_TYPE_OFFER || sdp == null) {
            console.error("Invalid or unsupported offer.")
            p2pState = P2pState.WAITING_FOR_OFFER
            return
        }

        p2pState = P2pState.CREATING_ANSWER
        scope.launch {
            operationMutex.withLock {
                runCatching {
                    resetConnection()
                    val connection = ensurePeerConnection()
                    connection.setRemoteDescription(WebRtc.SessionDescription(
                        type = WebRtc.SessionDescriptionType.OFFER,
                        sdp = sdp
                    ))
                    val answer = connection.createAnswer()
                    connection.setLocalDescription(answer)
                    connection.awaitIceGatheringComplete()

                    val localDescription = connection.localDescription ?: answer
                    console.printf("Here is your answer:")
                    console.success(serializeDescription(localDescription))
                    p2pState = P2pState.WAITING_TO_CONNECT
                }.onFailure { error ->
                    console.error("Failed to process offer: ${error.message}")
                    p2pState = P2pState.WAITING_FOR_OFFER
                }
            }
        }
    }

    override fun processAnswer(sdpJSON: String) {
        val (type: String?, sdp: String?) = deserializeSdp(sdpJSON, console)
        if (type?.lowercase() != SDP_TYPE_ANSWER || sdp == null) {
            console.error("Invalid or unsupported answer.")
            p2pState = P2pState.WAITING_FOR_ANSWER
            return
        }

        p2pState = P2pState.WAITING_TO_CONNECT
        scope.launch {
            operationMutex.withLock {
                runCatching {
                    val connection = peerConnection
                        ?: error("Peer connection is not initialized. Create an offer first.")
                    connection.setRemoteDescription(WebRtc.SessionDescription(
                        type = WebRtc.SessionDescriptionType.ANSWER,
                        sdp = sdp
                    ))
                }.onFailure { error ->
                    console.error("Failed to process answer: ${error.message}")
                    p2pState = P2pState.WAITING_FOR_ANSWER
                }
            }
        }
    }

    override fun makeOffer() {
        p2pState = P2pState.CREATING_OFFER
        scope.launch {
            operationMutex.withLock {
                runCatching {
                    resetConnection()
                    val connection = ensurePeerConnection()
                    ensureDataChannel(connection)

                    val offer = connection.createOffer()
                    connection.setLocalDescription(offer)
                    connection.awaitIceGatheringComplete()

                    val localDescription = connection.localDescription ?: offer
                    console.printf("Your offer is:")
                    console.success(serializeDescription(localDescription))
                    p2pState = P2pState.WAITING_FOR_ANSWER
                }.onFailure { error ->
                    console.error("Failed to create offer: ${error.message}")
                    p2pState = P2pState.WAITING_FOR_OFFER
                }
            }
        }
    }

    override fun sendMessage(message: String) {
        scope.launch {
            operationMutex.withLock {
                val channel = dataChannel
                if (channel != null && p2pState == P2pState.CHAT_ESTABLISHED) {
                    runCatching {
                        channel.send(serializeMessage(message))
                    }.onFailure { error ->
                        console.error("Failed to send message: ${error.message}")
                    }
                } else {
                    console.error("Error. Chat is not established.")
                }
            }
        }
    }

    override fun makeDataChannel() {
        scope.launch {
            operationMutex.withLock {
                runCatching {
                    val connection = peerConnection ?: error("Peer connection is not initialized.")
                    ensureDataChannel(connection)
                }.onFailure { error ->
                    console.error("Failed to create data channel: ${error.message}")
                }
            }
        }
    }

    override fun destroy() {
        resetConnection()
        runCatching { webRtcClient.close() }
        scope.cancel()
    }

    private suspend fun ensurePeerConnection(): WebRtcPeerConnection {
        val cachedConnection = peerConnection
        if (cachedConnection != null) {
            return cachedConnection
        }

        val newConnection = webRtcClient.createPeerConnection {
            iceServers = listOf(WebRtc.IceServer(DEFAULT_STUN_SERVER))
        }
        observeConnection(newConnection)
        peerConnection = newConnection
        return newConnection
    }

    private suspend fun ensureDataChannel(connection: WebRtcPeerConnection) {
        if (dataChannel != null) return
        val channel = connection.createDataChannel(DEFAULT_DATA_CHANNEL_LABEL)
        attachDataChannel(channel)
    }

    private fun observeConnection(connection: WebRtcPeerConnection) {
        connectionEventsJob?.cancel()
        connectionEventsJob = scope.launch {
            launch {
                connection.iceCandidates.collectLatest { candidate ->
                    console.debug("ice candidate:{${candidate.candidate}}")
                }
            }
            launch {
                connection.iceGatheringState.collectLatest { state ->
                    console.debug("ice gathering state change:${state.name}")
                }
            }
            launch {
                connection.iceConnectionState.collectLatest { state ->
                    console.debug("ice connection state change:${state.name}")
                    if (state == WebRtc.IceConnectionState.DISCONNECTED ||
                        state == WebRtc.IceConnectionState.CLOSED
                    ) {
                        handleChannelClosed()
                    }
                }
            }
            launch {
                connection.state.collectLatest { state ->
                    console.debug("connection state change:${state.name}")
                }
            }
            launch {
                connection.signalingState.collectLatest { state ->
                    console.debug("signaling state change:${state.name}")
                }
            }
            launch {
                connection.negotiationNeeded.collectLatest {
                    console.debug("renegotiation needed")
                }
            }
            launch {
                connection.dataChannelEvents.collectLatest { event ->
                    onDataChannelEvent(event, connection)
                }
            }
        }
    }

    private fun onDataChannelEvent(
        event: DataChannelEvent,
        connection: WebRtcPeerConnection,
    ) {
        when (event) {
            is DataChannelEvent.Open -> {
                attachDataChannel(event.channel)
                p2pState = P2pState.CHAT_ESTABLISHED
                console.success("Chat established.")
                val remoteAddress = connection.remoteDescription?.sdp ?: "unknown"
                console.printf("Connected to remote peer: $remoteAddress")
            }
            is DataChannelEvent.Closing -> {
                if (dataChannel === event.channel) {
                    console.debug("Channel closing")
                }
            }
            is DataChannelEvent.Closed -> {
                if (dataChannel === event.channel) {
                    handleChannelClosed()
                }
            }
            is DataChannelEvent.BufferedAmountLow -> {
                if (dataChannel === event.channel) {
                    console.debug("channel buffered amount changed:${event.channel.bufferedAmount}")
                }
            }
            is DataChannelEvent.Error -> {
                if (dataChannel === event.channel) {
                    console.error("Data channel error: ${event.reason}")
                }
            }
        }
    }

    private fun attachDataChannel(channel: WebRtcDataChannel) {
        dataChannel = channel
        startChannelReceiveLoop(channel)
    }

    private fun startChannelReceiveLoop(channel: WebRtcDataChannel) {
        channelReceiveJob?.cancel()
        channelReceiveJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                while (true) {
                    val payload = channel.receive()
                    val raw = payload.textOrNull() ?: payload.binaryOrNull()?.decodeToString()
                    if (raw == null) {
                        console.error("Malformed message received")
                        continue
                    }
                    val message: String? = deserializeMessage(raw, console)
                    if (message != null) {
                        console.warning(">$message")
                    } else {
                        console.error("Malformed message received")
                    }
                }
            } catch (_: CancellationException) {
                // Ignore cancellation because it's a normal part of channel lifecycle.
            } catch (error: Throwable) {
                console.error("Data channel receive failed: ${error.message}")
            }
        }
    }

    private fun handleChannelClosed() {
        if (p2pState != P2pState.CHAT_ENDED) {
            p2pState = P2pState.CHAT_ENDED
            console.error("Chat ended.")
        }
    }

    private fun resetConnection() {
        channelReceiveJob?.cancel()
        channelReceiveJob = null

        connectionEventsJob?.cancel()
        connectionEventsJob = null

        dataChannel?.close()
        dataChannel = null

        peerConnection?.close()
        peerConnection = null
    }

    private fun serializeDescription(description: WebRtc.SessionDescription): String {
        val type = when (description.type) {
            WebRtc.SessionDescriptionType.OFFER -> SDP_TYPE_OFFER
            WebRtc.SessionDescriptionType.ANSWER -> SDP_TYPE_ANSWER
            WebRtc.SessionDescriptionType.PROVISIONAL_ANSWER -> "pranswer"
            WebRtc.SessionDescriptionType.ROLLBACK -> "rollback"
        }
        return serializeSdp(type, description.sdp)
    }

    private companion object {
        private const val DEFAULT_STUN_SERVER = "stun:stun.l.google.com:19302"
        private const val DEFAULT_DATA_CHANNEL_LABEL = "test"
        private const val SDP_TYPE_OFFER = "offer"
        private const val SDP_TYPE_ANSWER = "answer"
    }
}
