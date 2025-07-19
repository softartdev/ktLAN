package com.softartdev.ktlan.data.webrtc

import dev.onvoid.webrtc.SetSessionDescriptionObserver

open class DefaultSetDescObserver(private val console: IConsole) : SetSessionDescriptionObserver {
    override fun onSuccess() {
        console.i("set success")
    }

    override fun onFailure(error: String?) {
        console.e("set failure:$error")
    }
}
