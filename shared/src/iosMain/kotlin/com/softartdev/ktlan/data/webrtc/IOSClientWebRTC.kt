@file:OptIn(ExperimentalKtorApi::class)

package com.softartdev.ktlan.data.webrtc

import io.ktor.client.webrtc.IosWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

class IOSClientWebRTC : KtorServerlessRTCClient(
    webRtcClient = WebRtcClient(IosWebRtc)
)
