@file:OptIn(ExperimentalKtorApi::class)

package com.softartdev.ktlan.data.webrtc

import android.content.Context
import io.ktor.client.webrtc.AndroidWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

class AndroidClientWebRTC(
    context: Context,
) : KtorServerlessRTCClient(
    webRtcClient = WebRtcClient(AndroidWebRtc) {
        this.context = context.applicationContext
    }
)
