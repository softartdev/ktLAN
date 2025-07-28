package com.softartdev.ktlan.domain.model

/** Simple chat message with sender information. */
data class ChatMessage(
    val sender: Sender,
    val text: String,
    val timestamp: Long
) {
    enum class Sender { Local, Remote }
}
