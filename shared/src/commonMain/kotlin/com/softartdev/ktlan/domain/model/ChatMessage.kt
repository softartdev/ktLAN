package com.softartdev.ktlan.domain.model

/**
 * Single chat message with sender side and timestamp.
 */
data class ChatMessage(
    val sender: Sender,
    val text: String,
    val timestamp: Long,
) {
    enum class Sender { Local, Remote }
}
