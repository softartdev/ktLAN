package com.softartdev.ktlan.presentation.socket

import com.softartdev.ktlan.domain.model.ChatMessage

/**
 * View state for socket chat screen.
 */
data class SocketResult(
    val loading: Boolean = false,
    val serverRunning: Boolean = false,
    val connected: Boolean = false,
    val bindHost: String = "0.0.0.0",
    val bindPort: String = "51337",
    val remoteHost: String = "",
    val remotePort: String = "51337",
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val error: String? = null,
)
