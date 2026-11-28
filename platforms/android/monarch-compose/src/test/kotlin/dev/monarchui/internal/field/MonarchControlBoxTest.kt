package dev.monarchui.internal.field

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.test.Test

/** monarchControlBox draws a 16dp box inside a 48dp touch target and keeps the toggle semantics of its parent. */
@RunWith(RobolectricTestRunner::class)
class MonarchControlBoxTest {
    @get:Rule
    val rule = createComposeRule()

    private fun setCheck(fontScale: Float = 1f) {
        rule.setMonarchContent(fontScale = fontScale) {
            var checked by remember { mutableStateOf(false) }
            val colors = MonarchTheme.colors
            Box(
                Modifier
                    .testTag("check")
                    .toggleable(value = checked, onValueChange = { checked = it }, role = Role.Checkbox)
                    .monarchMinimumTouchTarget(),
            ) {
                Box(
                    Modifier
                        .testTag("visible")
                        .monarchControlBox(
                            MonarchControlMetrics.CheckSize,
                            MonarchShapes.Default.xs,
                            if (checked) colors.accentStrong else colors.bgSurface,
                            colors.borderInput,
                        ),
                )
            }
        }
    }

    @Test
    fun theVisibleBoxIs16DpInsideA48DpTarget() {
        setCheck()
        rule.onNodeWithTag("visible", useUnmergedTree = true).assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(16.dp)
        rule.onNodeWithTag("check").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theTargetStaysAtFontScale2() {
        setCheck(LargestFontScale)
        rule.onNodeWithTag("check").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theParentToggleStillToggles() {
        setCheck()
        rule.onNodeWithTag("check").performClick().assertIsOn()
    }
}
