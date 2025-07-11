package com.softartdev.ktlan.data.webrtc

import io.github.aakira.napier.Napier

class LogConsole : IConsole {
    override fun printf(text: String, vararg args: Any) {
        val formattedText: String = buildString {
            append(text)
            if (args.isNotEmpty()) {
                append(" ")
                append(args.joinToString(" "))
            }
        }
        Napier.d(message = formattedText)
    }

    override fun d(text: String, vararg args: Any) {
        printf(text = "⚪️ $text", args = args)
    }

    override fun i(text: String, vararg args: Any) {
        greenf(text, args)
    }

    override fun e(text: String, vararg args: Any) {
        redf(text, args)
    }

    override fun greenf(text: String, vararg args: Any) {
        printf(text = "🟢 $text", args = args)
    }

    override fun bluef(text: String, vararg args: Any) {
        printf(text = "🔵 $text", args = args)
    }

    override fun redf(text: String, vararg args: Any) {
        printf(text = "🔴 $text", args = args)
    }
}