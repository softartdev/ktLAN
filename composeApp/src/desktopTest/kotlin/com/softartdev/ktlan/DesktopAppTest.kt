package com.softartdev.ktlan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiTestModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.logger.Level

class DesktopAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Napier.base(antilog = DebugAntilog())
        when (GlobalContext.getKoinApplicationOrNull()) {
            null -> startKoin {
                printLogger(level = Level.DEBUG)
                modules(sharedModules + uiTestModules)
            }
            else -> loadKoinModules(sharedModules + uiTestModules)
        }
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Swing)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                App()
            }
        }
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        unloadKoinModules(sharedModules + uiTestModules)
        Napier.takeLogarithm()
    }

    @Test
    fun appLaunches() {
        composeTestRule.onNodeWithText("Connection").assertExists()
        composeTestRule.onNodeWithText("Scan").assertExists()
        composeTestRule.onNodeWithText("Networks").assertExists()
        composeTestRule.onNodeWithText("LAN Chat").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }
}
