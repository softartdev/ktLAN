package com.softartdev.ktlan.test_util

import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher

class CoroutineDispatchersStub(testDispatcher: TestDispatcher) : CoroutineDispatchers {

    constructor(scheduler: TestCoroutineScheduler = TestCoroutineScheduler()) : this(
        testDispatcher = StandardTestDispatcher(scheduler)
    )
    override val default: CoroutineDispatcher = testDispatcher
    override val main: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
}
