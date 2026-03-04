package com.softartdev.ktlan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiTestModules
import com.softartdev.ktlan.main.MainBottomTab
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
        Logger.setLogWriters(platformLogWriter())
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
        Logger.setLogWriters(emptyList())
    }

    @Test
    fun appLaunches() {
        MainBottomTab.entries.asSequence()
            .map(MainBottomTab::testTag)
            .map(composeTestRule::onNodeWithTag)
            .forEach(SemanticsNodeInteraction::assertExists)
    }
}
