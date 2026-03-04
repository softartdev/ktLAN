@file:OptIn(ExperimentalKtorApi::class)

package com.softartdev.ktlan.data.webrtc

import io.ktor.client.webrtc.JsWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

class WasmJsClientWebRTC : KtorServerlessRTCClient(
    webRtcClient = WebRtcClient(JsWebRtc)
)
