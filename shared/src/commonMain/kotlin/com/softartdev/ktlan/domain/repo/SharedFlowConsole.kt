@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.data.webrtc.IConsole
import com.softartdev.ktlan.domain.model.ConsoleMessage
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SharedFlowConsole(
    private val mutableSharedFlow: MutableSharedFlow<ConsoleMessage>,
    coroutineDispatchers: CoroutineDispatchers
) : IConsole {

    val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatchers.default)

    fun print(consoleMessage: ConsoleMessage) {
        coroutineScope.launch { mutableSharedFlow.emit(consoleMessage) }
    }

    override fun printf(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "📢",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )

    override fun debug(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "⚪️",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )

    override fun info(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "ℹ️",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )

    override fun error(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "❌",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )

    override fun success(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "✅",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )

    override fun warning(text: String, vararg args: Any) = print(
        consoleMessage = ConsoleMessage(
            leading = "⚠️",
            overline = Clock.System.now().toString(),
            headline = text,
            supporting = args.joinToString(),
            trailing = "🦄"
        )
    )
}