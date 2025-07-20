import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.softartdev.ktlan.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.junit.Rule
import org.junit.Test

class DesktopAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appLaunches() {
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Swing)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                App()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Scan").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }
}
