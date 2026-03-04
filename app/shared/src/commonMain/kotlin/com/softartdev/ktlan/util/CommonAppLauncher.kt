package com.softartdev.ktlan.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import co.touchlab.kermit.platformLogWriter
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiModules
import org.koin.core.context.startKoin
import org.koin.core.logger.KOIN_TAG
import org.koin.core.logger.Level
import org.koin.dsl.KoinConfiguration

object CommonAppLauncher {
    private var launched: Boolean = false

    fun launch(
        debug: Boolean = true,
        koinConfig: KoinConfiguration? = null // tests or Android context
    ) {
        if (launched) {
            Logger.w("App already launched, skipping re-initialization.")
            return
        }
        Logger.setLogWriters(platformLogWriter())
        Logger.setTag("ktLAN")
        startKoin {
            val kermitKoinLogger = KermitKoinLogger(Logger.withTag(KOIN_TAG))
            if (debug) kermitKoinLogger.level = Level.DEBUG
            logger(kermitKoinLogger)
            koinConfig?.appDeclaration?.invoke(this@startKoin)
            modules(sharedModules + uiModules)
        }
        launched = true
    }
}
