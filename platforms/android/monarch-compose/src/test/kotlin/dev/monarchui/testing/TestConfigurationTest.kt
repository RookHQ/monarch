package dev.monarchui.testing

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The settings every test relies on (C-052): `robolectric.properties` pins
 * US English, and the test manifest supports right to left, so the windows of
 * dialogs and sheets follow an RTL layout direction too.
 */
@RunWith(RobolectricTestRunner::class)
class TestConfigurationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyTestRunsInUsEnglishWhateverTheLocaleOfTheMachine() {
        assertEquals(Locale.US, Locale.getDefault())
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Locale.US, context.resources.configuration.locales[0])
    }

    @Test
    fun theTestApplicationSupportsRightToLeft() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.applicationInfo.flags and ApplicationInfo.FLAG_SUPPORTS_RTL != 0)
    }

    @Test
    fun aDialogWindowLaysOutRightToLeftInAnRtlTest() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            Dialog(onDismissRequest = {}) {
                Row {
                    Text("first")
                    Text("second")
                }
            }
        }
        rule.onNodeWithText("first").assertIsBefore(rule.onNodeWithText("second"), LayoutDirection.Rtl)
    }
}
