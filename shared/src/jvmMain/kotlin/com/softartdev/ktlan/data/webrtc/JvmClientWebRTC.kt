@file:OptIn(ExperimentalKtorApi::class)

package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.CreateSessionDescriptionObserver
import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.PeerConnectionObserver
import dev.onvoid.webrtc.RTCAnswerOptions
import dev.onvoid.webrtc.RTCBundlePolicy
import dev.onvoid.webrtc.RTCConfiguration
import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.RTCDataChannelBuffer
import dev.onvoid.webrtc.RTCDataChannelInit
import dev.onvoid.webrtc.RTCDataChannelObserver
import dev.onvoid.webrtc.RTCDataChannelState
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.RTCIceConnectionState
import dev.onvoid.webrtc.RTCIceGatheringState
import dev.onvoid.webrtc.RTCIceServer
import dev.onvoid.webrtc.RTCIceTransportPolicy
import dev.onvoid.webrtc.RTCOfferOptions
import dev.onvoid.webrtc.RTCPeerConnection
import dev.onvoid.webrtc.RTCPeerConnectionState
import dev.onvoid.webrtc.RTCStatsReport
import dev.onvoid.webrtc.RTCRtcpMuxPolicy
import dev.onvoid.webrtc.RTCSdpType
import dev.onvoid.webrtc.RTCSessionDescription
import dev.onvoid.webrtc.RTCSignalingState
import dev.onvoid.webrtc.SetSessionDescriptionObserver
import io.ktor.client.webrtc.DataChannelEvent
import io.ktor.client.webrtc.DataChannelReceiveOptions
import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcClientEngineFactory
import io.ktor.client.webrtc.WebRtcConfig
import io.ktor.client.webrtc.WebRtcConnectionConfig
import io.ktor.client.webrtc.WebRtcConnectionEventsEmitter
import io.ktor.client.webrtc.WebRtcDataChannel
import io.ktor.client.webrtc.WebRtcDataChannelOptions
import io.ktor.client.webrtc.WebRtcEngine
import io.ktor.client.webrtc.WebRtcEngineBase
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JvmClientWebRTC : KtorServerlessRTCClient(
    webRtcClient = WebRtcClient(JvmWebRtc)
)

private class JvmWebRtcEngineConfig : WebRtcConfig() {
    var rtcFactory: PeerConnectionFactory? = null
}

private class JvmWebRtcEngine(
    override val config: JvmWebRtcEngineConfig,
) : WebRtcEngineBase("jvm-webrtc", config), MediaTrackFactory {
    private val ownsFactory: Boolean = config.rtcFactory == null
    private val localFactory: PeerConnectionFactory = config.rtcFactory ?: PeerConnectionFactory()

    override suspend fun createAudioTrack(
        constraints: WebRtcMedia.AudioTrackConstraints,
    ): WebRtcMedia.AudioTrack = error("Audio track creation is not supported by the desktop WebRTC adapter.")

    override suspend fun createVideoTrack(
        constraints: WebRtcMedia.VideoTrackConstraints,
    ): WebRtcMedia.VideoTrack = error("Video track creation is not supported by the desktop WebRTC adapter.")

    override suspend fun createPeerConnection(config: WebRtcConnectionConfig): WebRtcPeerConnection {
        val nativeConfig = RTCConfiguration().apply {
            iceServers.addAll(config.iceServers.map { it.toNative() })
            bundlePolicy = config.bundlePolicy.toNative()
            rtcpMuxPolicy = config.rtcpMuxPolicy.toNative()
            iceTransportPolicy = config.iceTransportPolicy.toNative()
        }

        val coroutineContext = createConnectionContext(config.exceptionHandler)
        return JvmWebRtcPeerConnection(coroutineContext, config) { observer ->
            localFactory.createPeerConnection(nativeConfig, observer)
        }
    }

    override fun close() {
        super.close()
        if (ownsFactory) {
            localFactory.dispose()
        }
    }
}

private object JvmWebRtc : WebRtcClientEngineFactory<JvmWebRtcEngineConfig> {
    override fun create(block: JvmWebRtcEngineConfig.() -> Unit): WebRtcEngine =
        JvmWebRtcEngine(JvmWebRtcEngineConfig().apply(block))
}

private class JvmWebRtcPeerConnection(
    coroutineContext: CoroutineContext,
    config: WebRtcConnectionConfig,
    createConnection: (PeerConnectionObserver) -> RTCPeerConnection,
) : WebRtcPeerConnection(coroutineContext, config) {
    private val peerConnection: RTCPeerConnection = createConnection(createObserver())

    private fun createObserver(): PeerConnectionObserver = object : PeerConnectionObserver {
        override fun onIceCandidate(candidate: RTCIceCandidate) = runInConnectionScope {
            events.emitIceCandidate(candidate.toKtor())
        }

        override fun onIceConnectionChange(state: RTCIceConnectionState) = runInConnectionScope {
            events.emitIceConnectionStateChange(state.toKtor())
        }

        override fun onConnectionChange(state: RTCPeerConnectionState) = runInConnectionScope {
            events.emitConnectionStateChange(state.toKtor())
        }

        override fun onIceGatheringChange(state: RTCIceGatheringState) = runInConnectionScope {
            events.emitIceGatheringStateChange(state.toKtor())
        }

        override fun onSignalingChange(state: RTCSignalingState) = runInConnectionScope {
            events.emitSignalingStateChange(state.toKtor())
        }

        override fun onRenegotiationNeeded() = runInConnectionScope {
            events.emitNegotiationNeeded()
        }

        override fun onDataChannel(dataChannel: RTCDataChannel) = runInConnectionScope {
            val channel = JvmWebRtcDataChannel(
                nativeChannel = dataChannel,
                coroutineScope = coroutineScope,
                receiveOptions = DataChannelReceiveOptions()
            )
            channel.setupEvents(events)
        }
    }

    override val localDescription: WebRtc.SessionDescription?
        get() = peerConnection.localDescription?.toKtor()

    override val remoteDescription: WebRtc.SessionDescription?
        get() = peerConnection.remoteDescription?.toKtor()

    override suspend fun createOffer(): WebRtc.SessionDescription {
        val offer = suspendCancellableCoroutine { cont ->
            peerConnection.createOffer(RTCOfferOptions(), cont.resumeAfterSdpCreate())
        }
        return offer.toKtor()
    }

    override suspend fun createAnswer(): WebRtc.SessionDescription {
        val answer = suspendCancellableCoroutine { cont ->
            peerConnection.createAnswer(RTCAnswerOptions(), cont.resumeAfterSdpCreate())
        }
        return answer.toKtor()
    }

    override suspend fun createDataChannel(
        label: String,
        options: WebRtcDataChannelOptions.() -> Unit,
    ): WebRtcDataChannel {
        val config = WebRtcDataChannelOptions().apply(options)
        val channelInit = RTCDataChannelInit().apply {
            config.id?.let { id = it }
            config.maxRetransmits?.let { maxRetransmits = it }
            config.maxPacketLifeTime?.let { maxPacketLifeTime = it.inWholeMilliseconds.toInt() }
            ordered = config.ordered
            protocol = config.protocol
            negotiated = config.negotiated
        }

        val nativeChannel = peerConnection.createDataChannel(label, channelInit)
        val receiveOptions = DataChannelReceiveOptions().apply(config.receiveOptions)
        return JvmWebRtcDataChannel(nativeChannel, coroutineScope, receiveOptions).also {
            it.setupEvents(events)
        }
    }

    override suspend fun setLocalDescription(description: WebRtc.SessionDescription) {
        suspendCancellableCoroutine { cont ->
            peerConnection.setLocalDescription(description.toNative(), cont.resumeAfterSdpSet())
        }
    }

    override suspend fun setRemoteDescription(description: WebRtc.SessionDescription) {
        suspendCancellableCoroutine { cont ->
            peerConnection.setRemoteDescription(description.toNative(), cont.resumeAfterSdpSet())
        }
    }

    override suspend fun addIceCandidate(candidate: WebRtc.IceCandidate) {
        peerConnection.addIceCandidate(candidate.toNative())
    }

    override suspend fun addTrack(track: WebRtcMedia.Track): WebRtc.RtpSender =
        error("Track sending is not supported by the desktop WebRTC adapter.")

    override suspend fun removeTrack(sender: WebRtc.RtpSender) {
        error("Track sending is not supported by the desktop WebRTC adapter.")
    }

    override suspend fun removeTrack(track: WebRtcMedia.Track) {
        error("Track sending is not supported by the desktop WebRTC adapter.")
    }

    override fun restartIce() {
        peerConnection.restartIce()
    }

    override suspend fun getStatistics(): List<WebRtc.Stats> = suspendCancellableCoroutine { cont ->
        peerConnection.getStats { report ->
            cont.resume(report.toKtor())
        }
    }

    override fun close() {
        super.close()
        peerConnection.close()
    }
}

private class JvmWebRtcDataChannel(
    private val nativeChannel: RTCDataChannel,
    private val coroutineScope: CoroutineScope,
    receiveOptions: DataChannelReceiveOptions,
) : WebRtcDataChannel(receiveOptions) {
    override val id: Int?
        get() = nativeChannel.id.takeIf { it >= 0 }

    override val label: String
        get() = nativeChannel.label

    override val state: WebRtc.DataChannel.State
        get() = nativeChannel.state.toKtor()

    override val bufferedAmount: Long
        get() = nativeChannel.bufferedAmount

    override val maxPacketLifeTime: Int?
        get() = nativeChannel.maxPacketLifeTime.takeIf { it >= 0 }

    override val maxRetransmits: Int?
        get() = nativeChannel.maxRetransmits.takeIf { it >= 0 }

    override val negotiated: Boolean
        get() = nativeChannel.isNegotiated

    override val ordered: Boolean
        get() = nativeChannel.isOrdered

    override val protocol: String
        get() = nativeChannel.protocol ?: ""

    override var bufferedAmountLowThreshold: Long = 0
        private set

    override suspend fun send(text: String) {
        val payload = ByteBuffer.wrap(text.toByteArray(Charsets.UTF_8))
        nativeChannel.send(RTCDataChannelBuffer(payload, false))
    }

    override suspend fun send(bytes: ByteArray) {
        nativeChannel.send(RTCDataChannelBuffer(ByteBuffer.wrap(bytes), true))
    }

    override fun setBufferedAmountLowThreshold(threshold: Long) {
        bufferedAmountLowThreshold = threshold
    }

    override fun closeTransport() {
        nativeChannel.close()
    }

    override fun close() {
        super.close()
        nativeChannel.unregisterObserver()
        nativeChannel.dispose()
    }

    internal fun setupEvents(eventsEmitter: WebRtcConnectionEventsEmitter) {
        nativeChannel.registerObserver(object : RTCDataChannelObserver {
            override fun onBufferedAmountChange(previousAmount: Long) = runInConnectionScope {
                if (previousAmount >= bufferedAmountLowThreshold &&
                    bufferedAmount <= bufferedAmountLowThreshold
                ) {
                    eventsEmitter.emitDataChannelEvent(DataChannelEvent.BufferedAmountLow(this@JvmWebRtcDataChannel))
                }
            }

            override fun onStateChange() = runInConnectionScope {
                val event = when (state) {
                    WebRtc.DataChannel.State.CONNECTING -> null
                    WebRtc.DataChannel.State.OPEN -> DataChannelEvent.Open(this@JvmWebRtcDataChannel)
                    WebRtc.DataChannel.State.CLOSING -> DataChannelEvent.Closing(this@JvmWebRtcDataChannel)
                    WebRtc.DataChannel.State.CLOSED -> {
                        stopReceivingMessages()
                        DataChannelEvent.Closed(this@JvmWebRtcDataChannel)
                    }
                }
                if (event != null) {
                    eventsEmitter.emitDataChannelEvent(event)
                }
            }

            override fun onMessage(buffer: RTCDataChannelBuffer) = runInConnectionScope {
                emitMessage(buffer.toKtorMessage())
            }
        })
    }

    private inline fun runInConnectionScope(crossinline block: suspend () -> Unit) {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { block() }
    }
}

private fun RTCStatsReport.toKtor(): List<WebRtc.Stats> {
    return stats.values.map { stat ->
        WebRtc.Stats(
            id = stat.id,
            type = stat.type.name.lowercase(),
            timestamp = stat.timestamp,
            props = stat.attributes.mapValues { it.value }
        )
    }
}

private fun RTCDataChannelBuffer.toKtorMessage(): WebRtc.DataChannel.Message {
    val byteBuffer = data
    val byteArray = ByteArray(byteBuffer.remaining())
    byteBuffer.get(byteArray)
    return if (binary) {
        WebRtc.DataChannel.Message.Binary(byteArray)
    } else {
        WebRtc.DataChannel.Message.Text(byteArray.toString(Charsets.UTF_8))
    }
}

private fun RTCIceCandidate.toKtor(): WebRtc.IceCandidate = WebRtc.IceCandidate(
    candidate = sdp,
    sdpMid = sdpMid ?: "",
    sdpMLineIndex = sdpMLineIndex
)

private fun WebRtc.IceCandidate.toNative(): RTCIceCandidate = RTCIceCandidate(
    sdpMid,
    sdpMLineIndex,
    candidate
)

private fun RTCSessionDescription.toKtor(): WebRtc.SessionDescription = WebRtc.SessionDescription(
    type = sdpType.toKtor(),
    sdp = sdp
)

private fun WebRtc.SessionDescription.toNative(): RTCSessionDescription = RTCSessionDescription(
    type.toNative(),
    sdp
)

private fun RTCSdpType.toKtor(): WebRtc.SessionDescriptionType = when (this) {
    RTCSdpType.OFFER -> WebRtc.SessionDescriptionType.OFFER
    RTCSdpType.ANSWER -> WebRtc.SessionDescriptionType.ANSWER
    RTCSdpType.PR_ANSWER -> WebRtc.SessionDescriptionType.PROVISIONAL_ANSWER
    RTCSdpType.ROLLBACK -> WebRtc.SessionDescriptionType.ROLLBACK
}

private fun WebRtc.SessionDescriptionType.toNative(): RTCSdpType = when (this) {
    WebRtc.SessionDescriptionType.OFFER -> RTCSdpType.OFFER
    WebRtc.SessionDescriptionType.ANSWER -> RTCSdpType.ANSWER
    WebRtc.SessionDescriptionType.PROVISIONAL_ANSWER -> RTCSdpType.PR_ANSWER
    WebRtc.SessionDescriptionType.ROLLBACK -> RTCSdpType.ROLLBACK
}

private fun WebRtc.IceServer.toNative(): RTCIceServer = RTCIceServer().apply {
    urls = this@toNative.urls
    username = this@toNative.username
    password = this@toNative.credential
}

private fun WebRtc.BundlePolicy.toNative(): RTCBundlePolicy = when (this) {
    WebRtc.BundlePolicy.BALANCED -> RTCBundlePolicy.BALANCED
    WebRtc.BundlePolicy.MAX_BUNDLE -> RTCBundlePolicy.MAX_BUNDLE
    WebRtc.BundlePolicy.MAX_COMPAT -> RTCBundlePolicy.MAX_COMPAT
}

private fun WebRtc.RtcpMuxPolicy.toNative(): RTCRtcpMuxPolicy = when (this) {
    WebRtc.RtcpMuxPolicy.NEGOTIATE -> RTCRtcpMuxPolicy.NEGOTIATE
    WebRtc.RtcpMuxPolicy.REQUIRE -> RTCRtcpMuxPolicy.REQUIRE
}

private fun WebRtc.IceTransportPolicy.toNative(): RTCIceTransportPolicy = when (this) {
    WebRtc.IceTransportPolicy.ALL -> RTCIceTransportPolicy.ALL
    WebRtc.IceTransportPolicy.RELAY -> RTCIceTransportPolicy.RELAY
}

private fun RTCIceConnectionState.toKtor(): WebRtc.IceConnectionState = when (this) {
    RTCIceConnectionState.NEW -> WebRtc.IceConnectionState.NEW
    RTCIceConnectionState.CHECKING -> WebRtc.IceConnectionState.CHECKING
    RTCIceConnectionState.CONNECTED -> WebRtc.IceConnectionState.CONNECTED
    RTCIceConnectionState.COMPLETED -> WebRtc.IceConnectionState.COMPLETED
    RTCIceConnectionState.FAILED -> WebRtc.IceConnectionState.FAILED
    RTCIceConnectionState.DISCONNECTED -> WebRtc.IceConnectionState.DISCONNECTED
    RTCIceConnectionState.CLOSED -> WebRtc.IceConnectionState.CLOSED
}

private fun RTCPeerConnectionState.toKtor(): WebRtc.ConnectionState = when (this) {
    RTCPeerConnectionState.NEW -> WebRtc.ConnectionState.NEW
    RTCPeerConnectionState.CONNECTING -> WebRtc.ConnectionState.CONNECTING
    RTCPeerConnectionState.CONNECTED -> WebRtc.ConnectionState.CONNECTED
    RTCPeerConnectionState.DISCONNECTED -> WebRtc.ConnectionState.DISCONNECTED
    RTCPeerConnectionState.FAILED -> WebRtc.ConnectionState.FAILED
    RTCPeerConnectionState.CLOSED -> WebRtc.ConnectionState.CLOSED
}

private fun RTCIceGatheringState.toKtor(): WebRtc.IceGatheringState = when (this) {
    RTCIceGatheringState.NEW -> WebRtc.IceGatheringState.NEW
    RTCIceGatheringState.GATHERING -> WebRtc.IceGatheringState.GATHERING
    RTCIceGatheringState.COMPLETE -> WebRtc.IceGatheringState.COMPLETE
}

private fun RTCSignalingState.toKtor(): WebRtc.SignalingState = when (this) {
    RTCSignalingState.STABLE -> WebRtc.SignalingState.STABLE
    RTCSignalingState.HAVE_LOCAL_OFFER -> WebRtc.SignalingState.HAVE_LOCAL_OFFER
    RTCSignalingState.HAVE_LOCAL_PR_ANSWER -> WebRtc.SignalingState.HAVE_LOCAL_PROVISIONAL_ANSWER
    RTCSignalingState.HAVE_REMOTE_OFFER -> WebRtc.SignalingState.HAVE_REMOTE_OFFER
    RTCSignalingState.HAVE_REMOTE_PR_ANSWER -> WebRtc.SignalingState.HAVE_REMOTE_PROVISIONAL_ANSWER
    RTCSignalingState.CLOSED -> WebRtc.SignalingState.CLOSED
}

private fun RTCDataChannelState.toKtor(): WebRtc.DataChannel.State = when (this) {
    RTCDataChannelState.CONNECTING -> WebRtc.DataChannel.State.CONNECTING
    RTCDataChannelState.OPEN -> WebRtc.DataChannel.State.OPEN
    RTCDataChannelState.CLOSING -> WebRtc.DataChannel.State.CLOSING
    RTCDataChannelState.CLOSED -> WebRtc.DataChannel.State.CLOSED
}

private fun Continuation<RTCSessionDescription>.resumeAfterSdpCreate(): CreateSessionDescriptionObserver =
    object : CreateSessionDescriptionObserver {
        override fun onSuccess(description: RTCSessionDescription) {
            resume(description)
        }

        override fun onFailure(error: String?) {
            resumeWithException(WebRtc.SdpException(error))
        }
    }

private fun Continuation<Unit>.resumeAfterSdpSet(): SetSessionDescriptionObserver =
    object : SetSessionDescriptionObserver {
        override fun onSuccess() {
            resume(Unit)
        }

        override fun onFailure(error: String?) {
            resumeWithException(WebRtc.SdpException(error))
        }
    }
