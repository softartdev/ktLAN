package com.softartdev.ktlan.presentation.socket

import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.repo.SocketRepo
import com.softartdev.ktlan.presentation.navigation.Router
import com.softartdev.ktlan.data.socket.SocketTransport
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

private class TestDispatchers(val dispatcher: CoroutineDispatcher = StandardTestDispatcher()) : CoroutineDispatchers {
    override val default = dispatcher
    override val main = dispatcher
    override val unconfined = dispatcher
    override val io = dispatcher
}

private class FakeRepo(private val dispatchers: CoroutineDispatchers) : SocketRepo(SocketTransport(dispatchers), dispatchers) {
    val sent = mutableListOf<String>()
    private val _messages = MutableSharedFlow<ChatMessage>()
    override fun observeMessages() = _messages
    override suspend fun getLocalIp(): String = "192.168.0.1"
    override suspend fun startServer(bindHost: String, bindPort: Int) {}
    override suspend fun connectTo(remoteHost: String, remotePort: Int) {}
    override suspend fun send(text: String) { sent += text }
    override suspend fun stop() {}
    suspend fun emitIncoming(text: String) { _messages.emit(ChatMessage(ChatMessage.Sender.Remote, text, 0)) }
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
    @Test
    fun testInitialStatePrefilled() = runTest {
        val dispatchers = TestDispatchers(testScheduler)
        val repo = FakeRepo(dispatchers)
        val vm = SocketViewModel(FakeRouter(), repo, SocketTransport(dispatchers))
        vm.launch()
        advanceUntilIdle()
        assertEquals("192.168.0.1", vm.stateFlow.value.bindHost)
    }

    @Test
    fun testSendAppendsMessage() = runTest {
        val dispatchers = TestDispatchers(testScheduler)
        val repo = FakeRepo(dispatchers)
        val vm = SocketViewModel(FakeRouter(), repo, SocketTransport(dispatchers))
        vm.launch()
        vm.onAction(SocketAction.Send("hi"))
        advanceUntilIdle()
        assertEquals(listOf("hi"), repo.sent)
    }

    @Test
    fun testApplyQrPayloadValid() = runTest {
        val dispatchers = TestDispatchers(testScheduler)
        val repo = FakeRepo(dispatchers)
        val vm = SocketViewModel(FakeRouter(), repo, SocketTransport(dispatchers))
        vm.launch()
        vm.onAction(SocketAction.ApplyQrPayload("ktlan://tcp?host=1.2.3.4&port=5"))
        advanceUntilIdle()
        assertEquals("1.2.3.4", vm.stateFlow.value.remoteHost)
        assertEquals("5", vm.stateFlow.value.remotePort)
    }

    @Test
    fun testApplyQrPayloadInvalid() = runTest {
        val dispatchers = TestDispatchers(testScheduler)
        val repo = FakeRepo(dispatchers)
        val vm = SocketViewModel(FakeRouter(), repo, SocketTransport(dispatchers))
        vm.launch()
        vm.onAction(SocketAction.ApplyQrPayload("wrong"))
        advanceUntilIdle()
        assertEquals("Invalid QR data", vm.stateFlow.value.error)
    }
}
