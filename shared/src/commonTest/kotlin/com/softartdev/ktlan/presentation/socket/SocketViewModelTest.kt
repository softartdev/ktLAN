@file:OptIn(ExperimentalCoroutinesApi::class)

package com.softartdev.ktlan.presentation.socket

import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.repo.SocketRepo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import com.softartdev.ktlan.presentation.navigation.Router
import com.softartdev.ktlan.test_util.CoroutineDispatchersStub
import com.softartdev.ktlan.test_util.PrintAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private class FakeRepo(dispatchers: CoroutineDispatchers) : SocketRepo(SocketTransport(dispatchers), dispatchers) {
    val sent = mutableListOf<String>()
    private val _messages = MutableSharedFlow<ChatMessage>()
    override fun observeMessages() = _messages
    override suspend fun getLocalIp(): String = "192.168.0.1"
    override suspend fun startServer(bindHost: String, bindPort: Int) {}
    override suspend fun connectTo(remoteHost: String, remotePort: Int) {}
    override suspend fun send(text: String) { sent += text }
    override suspend fun stop() {}
}

private class FakeRouter : Router {
    var last: Any? = null
    override fun setController(navController: Any) {}
    override fun releaseController() {}
    override fun <T : Any> navigate(route: T) { last = route }
    override fun <T : Any> navigateClearingBackStack(route: T) { last = route }
    override fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean) = false
    override fun popBackStack() = false
}

class SocketViewModelTest {
    private var testDispatchers: CoroutineDispatchersStub? = null
    private var repo: FakeRepo? = null
    private var vm: SocketViewModel? = null
    
    @BeforeTest
    fun setup() = runTest {
        Napier.base(PrintAntilog())
        testDispatchers = CoroutineDispatchersStub()
        repo = FakeRepo(testDispatchers!!)
        vm = SocketViewModel(FakeRouter(), repo!!, SocketTransport(testDispatchers!!))
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
        vm!!.onAction(SocketAction.Send("hi"))
        advanceUntilIdle()
        assertEquals(listOf("hi"), repo!!.sent)
    }

    @Test
    fun testApplyQrPayloadValid() = runTest(timeout = 5.seconds) {
        vm!!.launch()
        vm!!.onAction(SocketAction.ApplyQrPayload("ktlan://tcp?host=1.2.3.4&port=5"))
        advanceUntilIdle()
        assertEquals("1.2.3.4", vm!!.stateFlow.value.remoteHost)
        assertEquals("5", vm!!.stateFlow.value.remotePort)
    }

    @Test
    fun testApplyQrPayloadInvalid() = runTest(timeout = 5.seconds) {
        vm!!.launch()
        vm!!.onAction(SocketAction.ApplyQrPayload("wrong"))
        advanceUntilIdle()
        assertEquals("Invalid QR data", vm!!.stateFlow.value.error)
    }
}
