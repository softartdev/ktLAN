package com.softartdev.ktlan.data.webrtc

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

abstract class ServerlessRTCClient(
    var console: IConsole = LogConsole(),
) {
    private val p2pMutableStateFlow: MutableStateFlow<P2pState?> = MutableStateFlow(
        value = P2pState.INITIALIZING
    )
    val p2pStateFlow: StateFlow<P2pState?> = p2pMutableStateFlow

    var p2pState: P2pState?
        internal set(value) {
            p2pMutableStateFlow.value = value
        }
        get() = p2pStateFlow.value

    /**
     * Wait for an offer to be entered by user.
     */
    fun waitForOffer() {
        p2pState = P2pState.WAITING_FOR_OFFER
    }

    /**
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    abstract fun processOffer(sdpJSON: String)

    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    abstract fun processAnswer(sdpJSON: String)

    /**
     * App creates the offer.
     */
    abstract fun makeOffer()

    /**
     * Sends message to other party.
     */
    abstract fun sendMessage(message: String)

    /**
     * Creates data channel for use when offer is created on this machine.
     */
    abstract fun makeDataChannel()

    /**
     * Clean up some resources.
     */
    abstract fun destroy()

    companion object {
        private const val JSON_TYPE: String = "type"
        private const val JSON_MESSAGE: String = "message"
        private const val JSON_SDP: String = "sdp"

        internal fun serializeSdp(type: String, sdp: String): String =
            Json.encodeToString<JsonObject>(value = buildJsonObject {
                put(key = JSON_TYPE, value = type)
                put(key = JSON_SDP, value = sdp)
            })

        internal fun deserializeSdp(sdpJSON: String, console: IConsole): Pair<String?, String?> {
            var (type: String?, sdp: String?) = null to null
            try {
                val jsonObj: JsonObject = Json.parseToJsonElement(sdpJSON).jsonObject
                type = jsonObj[JSON_TYPE]?.jsonPrimitive?.content
                sdp = jsonObj[JSON_SDP]?.jsonPrimitive?.content
            } catch (e: Exception) {
                Napier.e("Error deserializing SDP", e)
                console.redf("Error deserializing SDP: ${e.message}")
            }
            return type to sdp
        }

        internal fun serializeMessage(message: String): String = Json.encodeToString<JsonObject>(
            value = buildJsonObject { put(key = JSON_MESSAGE, value = message) }
        )

        internal fun deserializeMessage(json: String, console: IConsole): String? = try {
            Json.parseToJsonElement(json).jsonObject[JSON_MESSAGE]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Napier.e("Error deserializing message", e)
            console.redf("Error deserializing message: ${e.message}")
            null
        }
    }
}