package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.domain.model.HostModel
import kotlin.coroutines.CoroutineContext

class ScanRepoStub : ScanRepo() {

    override suspend fun scanRangeParallel(
        coroutineContext: CoroutineContext,
        startIp: String,
        endIp: String,
        ports: List<Int>
    ): List<HostModel> = emptyList() // Simplified for testing
}