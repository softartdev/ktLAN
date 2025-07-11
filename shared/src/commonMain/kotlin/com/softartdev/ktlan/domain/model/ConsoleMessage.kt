package com.softartdev.ktlan.domain.model

data class ConsoleMessage(
    val leading: String? = null,
    val overline: String? = null,
    val headline: String? = null,
    val supporting: String? = null,
    val trailing: String? = null
)