package com.softartdev.ktlan.data.socket

import com.softartdev.ktlan.domain.util.CoroutineDispatchers

/**
 * Detect device's local IPv4 address. Returns "0.0.0.0" if detection fails.
 */
expect suspend fun getLocalIp(dispatchers: CoroutineDispatchers): String
