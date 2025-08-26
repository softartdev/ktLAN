package com.softartdev.ktlan.util

import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinConfiguration

object CommonAppLauncher {
    private var launched: Boolean = false

    fun launch(
        debug: Boolean = true,
        koinConfig: KoinConfiguration? = null // tests or Android context
    ) {
        if (launched) {
            Napier.w("App already launched, skipping re-initialization.")
            return
        }
        if (debug) {
            Napier.base(antilog = DebugAntilog())
        } else {
            // Napier.base(antilog = CrashlyticsAntilog())
        }
        startKoin {
            logger(NapierKoinLogger(Level.DEBUG))
            koinConfig?.appDeclaration?.invoke(this@startKoin)
            modules(sharedModules + uiModules)
        }
        launched = true
    }
}