package com.softartdev.ktlan.domain.util

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import io.ktor.client.plugins.logging.Logger as KtorLogger

class KermitKtorLogger(
    private val severity: Severity,
    private val logger: KermitLogger
) : KtorLogger {

    override fun log(message: String) = logger.log(severity, logger.tag, null, message)
}
