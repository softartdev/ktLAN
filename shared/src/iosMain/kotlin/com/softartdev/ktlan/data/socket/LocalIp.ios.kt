package com.softartdev.ktlan.data.socket

import com.softartdev.ktlan.domain.util.CoroutineDispatchers

/** iOS fallback returning placeholder. */
actual suspend fun getLocalIp(dispatchers: CoroutineDispatchers): String = "0.0.0.0"
