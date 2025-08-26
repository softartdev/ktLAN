@file:OptIn(ExperimentalCoroutinesApi::class)

package com.softartdev.ktlan.presentation.socket

import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.repo.NetworkRepoStub
import com.softartdev.ktlan.domain.repo.SocketRepoStub
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.RouterStub
import com.softartdev.ktlan.domain.util.CoroutineDispatchersStub
import com.softartdev.ktlan.domain.util.PrintAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SocketViewModelTest {
    private var testDispatchers: CoroutineDispatchersStub? = null
    private var repo: SocketRepoStub? = null
    private var vm: SocketViewModel? = null
    
    @BeforeTest
    fun setup() = runTest {
        Napier.base(PrintAntilog())
        testDispatchers = CoroutineDispatchersStub()
        repo = SocketRepoStub(testDispatchers!!)
        val networksRepo = NetworkRepoStub(testDispatchers!!)
        vm = SocketViewModel(RouterStub(), repo!!, SocketTransport(testDispatchers!!), networksRepo, AppNavGraph.BottomTab.Socket())
    }

    @AfterTest
    fun tearDown() = runTest {
        Napier.takeLogarithm()
    }

    @Test
    fun exampleTest() = runTest {
        val actual = 2 + 2
        assertEquals(4, actual, "2 + 2 should equal 4")
    }

    @Test
    fun testUpdateLocalIp() = runTest(timeout = 5.seconds) {
        vm!!.updateLocalIp()
        advanceUntilIdle()
        assertEquals("192.168.0.1", vm!!.stateFlow.value.bindHost)
    }

    @Test
    fun testSendAppendsMessage() = runTest(timeout = 5.seconds) {
        vm!!.launch()
        vm!!.send("hi")
        advanceUntilIdle()
        assertEquals(listOf("hi"), repo!!.sent)
    }

    @Test
    fun testApplyQrPayloadValid() = runTest(timeout = 5.seconds) {
        vm!!.launch()
        vm!!.applyQr("ktlan://tcp?host=1.2.3.4&port=5")
        advanceUntilIdle()
        assertEquals("1.2.3.4", vm!!.stateFlow.value.remoteHost)
        assertEquals("5", vm!!.stateFlow.value.remotePort)
    }

    @Test
    fun testApplyQrPayloadInvalid() = runTest(timeout = 5.seconds) {
        vm!!.launch()
        vm!!.applyQr("wrong")
        advanceUntilIdle()
        assertEquals("Invalid QR data", vm!!.stateFlow.value.error)
    }
}
