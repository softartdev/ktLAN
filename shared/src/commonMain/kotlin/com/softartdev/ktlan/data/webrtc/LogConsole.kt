package com.softartdev.ktlan.data.webrtc

import co.touchlab.kermit.Logger

class LogConsole : IConsole {
    private val logger = Logger.withTag("IConsole")

    override fun printf(text: String, vararg args: Any) {
        val formattedText: String = buildString {
            append(text)
            if (args.isNotEmpty()) {
                append(" ")
                append(args.joinToString(" "))
            }
        }
        logger.d { formattedText }
    }

    override fun debug(text: String, vararg args: Any) {
        printf("⚪️ $text", *args)
    }

    override fun info(text: String, vararg args: Any) {
        printf("ℹ️ $text", *args)
    }

    override fun error(text: String, vararg args: Any) {
        printf("❌ $text", *args)
    }

    override fun success(text: String, vararg args: Any) {
        printf("✅ $text", *args)
    }

    override fun warning(text: String, vararg args: Any) {
        printf("⚠️ $text", *args)
    }
}