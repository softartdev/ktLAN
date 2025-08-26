package com.softartdev.ktlan.util

import io.github.aakira.napier.Napier
import org.koin.core.logger.KOIN_TAG
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE

class NapierKoinLogger(level: Level) : Logger(level) {

    override fun display(level: Level, msg: MESSAGE) {
        if (this.level > level) return
        logOnLevel(msg, level)
    }

    private fun logOnLevel(msg: MESSAGE, level: Level) = when (level) {
        Level.DEBUG -> Napier.d(msg, tag = KOIN_TAG)
        Level.INFO -> Napier.i(msg, tag = KOIN_TAG)
        Level.WARNING -> Napier.w(msg, tag = KOIN_TAG)
        Level.ERROR -> Napier.e(msg, tag = KOIN_TAG)
        Level.NONE -> { /* No logging for NONE level */ }
    }
}
