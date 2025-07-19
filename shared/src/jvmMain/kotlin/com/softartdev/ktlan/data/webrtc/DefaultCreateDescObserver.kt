package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.CreateSessionDescriptionObserver
import dev.onvoid.webrtc.RTCSessionDescription

open class DefaultCreateDescObserver(private val console: IConsole, private val onSuccess: (RTCSessionDescription) -> Unit) : CreateSessionDescriptionObserver {
    override fun onSuccess(description: RTCSessionDescription) {
        console.i("create success")
        onSuccess(description)
    }

    override fun onFailure(error: String?) {
        console.e("failed to create offer:$error")
    }
}
