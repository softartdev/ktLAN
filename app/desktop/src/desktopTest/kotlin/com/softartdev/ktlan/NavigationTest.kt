package com.softartdev.ktlan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.softartdev.ktlan.di.sharedModules
import com.softartdev.ktlan.di.uiTestModules
import com.softartdev.ktlan.main.MainBottomTab
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class NavigationTest {
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
    fun testSocketTabNavigation() = runTest {
        composeTestRule.onNodeWithTag(MainBottomTab.Socket.testTag).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(MainBottomTab.Socket.testTag).assertExists()
    }

    @Test
    fun testNetworksToSocketNavigation() = runTest {
        composeTestRule.onNodeWithTag(MainBottomTab.Networks.testTag).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(MainBottomTab.Socket.testTag).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(MainBottomTab.Socket.testTag).assertExists()
    }

    @Test
    fun testAllTabNavigation() = runTest {
        val tabTags: List<String> = MainBottomTab.entries.map(MainBottomTab::testTag)
        for (tabTag in tabTags) {
            composeTestRule.onNodeWithTag(tabTag).performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun testNetworksToSocketNavigationViaUseButton() = runTest {
        val router: Router by KoinJavaComponent.inject(Router::class.java)

        composeTestRule.onNodeWithTag(MainBottomTab.Networks.testTag).performClick()
        composeTestRule.waitForIdle()

        captureScreenshot(composeTestRule, "networks_tab.png")

        router.bottomNavigate(AppNavGraph.BottomTab.Socket(remoteHost = "1.2.3.4"))
        composeTestRule.waitForIdle()

        captureScreenshot(composeTestRule, "socket_tab.png")
        
        composeTestRule.onNodeWithText("1.2.3.4").assertExists()
    }

    @Test
    fun testScanTabStateNotUpdatedWhenAlreadyOpen() = runTest {
        val router: Router by KoinJavaComponent.inject(Router::class.java)

        // First, navigate to Scan tab (should have default values)
        composeTestRule.onNodeWithTag(MainBottomTab.Scan.testTag).performClick()
        composeTestRule.waitForIdle()

        // Verify default values are present
        composeTestRule.onNodeWithText("192.168.0.100").assertExists() // Default start IP
        composeTestRule.onNodeWithText("192.168.0.125").assertExists() // Default end IP

        // Navigate to Networks tab
        composeTestRule.onNodeWithTag(MainBottomTab.Networks.testTag).performClick()
        composeTestRule.waitForIdle()

        // Use "Scan" button to navigate back to Scan with new parameters
        // This should update the TextFields but currently doesn't
        router.bottomNavigate(AppNavGraph.BottomTab.Scan(startIp = "10.0.0.1", endIp = "10.0.0.255"))
        composeTestRule.waitForIdle()

        // This test should fail - the TextFields should show new values but they don't
        composeTestRule.onNodeWithText("10.0.0.1").assertExists() // Should be new start IP
        composeTestRule.onNodeWithText("10.0.0.255").assertExists() // Should be new end IP
    }

    @Test
    fun testSocketTabStateNotUpdatedWhenAlreadyOpen() = runTest {
        val router: Router by KoinJavaComponent.inject(Router::class.java)

        // First, navigate to Socket tab (should have empty/default values)
        composeTestRule.onNodeWithTag(MainBottomTab.Socket.testTag).performClick()
        composeTestRule.waitForIdle()

        // Navigate to Networks tab
        composeTestRule.onNodeWithTag(MainBottomTab.Networks.testTag).performClick()
        composeTestRule.waitForIdle()

        // Use "Use" button to navigate back to Socket with new parameters
        router.bottomNavigate(AppNavGraph.BottomTab.Socket(remoteHost = "192.168.1.100"))
        composeTestRule.waitForIdle()

        // This test should fail - the remote host field should show new value but it doesn't
        composeTestRule.onNodeWithText("192.168.1.100").assertExists() // Should be new remote host
    }

    @Test
    fun testScanTabStateUpdatedWhenNavigatingFromDifferentTab() = runTest {
        val router: Router by KoinJavaComponent.inject(Router::class.java)

        // Navigate to Networks tab first
        composeTestRule.onNodeWithTag(MainBottomTab.Networks.testTag).performClick()
        composeTestRule.waitForIdle()

        // Use "Scan" button to navigate to Scan with parameters
        router.bottomNavigate(AppNavGraph.BottomTab.Scan(startIp = "172.16.0.1", endIp = "172.16.0.255"))
        composeTestRule.waitForIdle()

        // This should work - navigating from a different tab
        composeTestRule.onNodeWithText("172.16.0.1").assertExists() // Should be new start IP
        composeTestRule.onNodeWithText("172.16.0.255").assertExists() // Should be new end IP
    }
}

/**
 * Captures a screenshot of the entire composable content and saves it to a file.
 *
 * @param rule The [ComposeTestRule] to use for capturing.
 * @param filename The name of the file to save (e.g., "screenshot.png").
 */
fun captureScreenshot(rule: ComposeTestRule, filename: String) {
    val imageBitmap: ImageBitmap = rule.onRoot().captureToImage()
    val bufferedImage: BufferedImage = imageBitmap.toAwtImage()

    val outputDir = File("build/test-screenshots")
    if (!outputDir.exists()) outputDir.mkdirs()
    val file = File(outputDir, filename)

    ImageIO.write(bufferedImage, "PNG", file)
    Logger.d { "Screenshot saved to ${file.absolutePath}" }
}
