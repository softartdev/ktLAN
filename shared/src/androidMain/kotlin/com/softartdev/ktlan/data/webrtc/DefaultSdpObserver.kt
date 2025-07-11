package com.softartdev.ktlan.data.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class DefaultSdpObserver(private val console: IConsole) : SdpObserver {

    override fun onCreateSuccess(p0: SessionDescription?) {}

    override fun onCreateFailure(p0: String?) {
        console.e("failed to create offer:$p0")
    }

    override fun onSetFailure(p0: String?) {
        console.e("set failure:$p0")
    }

    override fun onSetSuccess() {
        console.i("set success")
    }
}