package com.softartdev.ktlan

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

object AppState {
    private var launched: Boolean = false

    fun launch() {
        if (launched) return

        Napier.base(antilog = DebugAntilog())

        launched = true
    }
}