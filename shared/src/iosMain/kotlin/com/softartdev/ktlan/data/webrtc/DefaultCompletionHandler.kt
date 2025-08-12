@file:OptIn(ExperimentalForeignApi::class)

package com.softartdev.ktlan.data.webrtc

import cocoapods.WebRTC.RTCSetSessionDescriptionCompletionHandler
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError

class DefaultCompletionHandler(
    private val console: IConsole
) : RTCSetSessionDescriptionCompletionHandler {

    override fun invoke(nsError: NSError?) {
        if (nsError == null) {
            console.i("set success")
        } else {
            console.e("set failure: ${nsError.localizedDescription}")
        }
    }
}