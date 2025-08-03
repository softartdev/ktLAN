package com.softartdev.ktlan.data.webrtc

class WasmJsClientWebRTC() : ServerlessRTCClient() {
    override fun processOffer(sdpJSON: String) = console.d("processOffer()") // TODO
    override fun processAnswer(sdpJSON: String) = console.d("processAnswer()") // TODO
    override fun makeOffer() = console.d("makeOffer()") // TODO
    override fun sendMessage(message: String) = console.d("sendMessage()") // TODO
    override fun makeDataChannel() = console.d("makeDataChannel()") // TODO
    override fun destroy() = console.d("destroy()") // TODO
}