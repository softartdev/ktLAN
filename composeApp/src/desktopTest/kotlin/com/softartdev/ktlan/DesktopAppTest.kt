import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle
import org.junit.Rule
import org.junit.Test

class DesktopAppTest {
    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
        }
    }
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appLaunches() {
        val lifecycleOwner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle
                get() = registry
        }
        composeTestRule.runOnUiThread {
            lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        }

        composeTestRule.setContent {
            androidx.compose.ui.window.Window(onCloseRequest = {}) {
                CompositionLocalProvider(
                    androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
                ) {
                    com.softartdev.ktlan.App()
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Scan").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }
}
