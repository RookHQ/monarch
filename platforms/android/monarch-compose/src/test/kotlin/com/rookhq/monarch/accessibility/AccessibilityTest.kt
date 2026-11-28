package com.rookhq.monarch.accessibility

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchThemeTokens
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMinimumTouchTargetIs48dp() {
        assertEquals(48.dp, MonarchTouchTarget.Minimum)
    }

    @Test
    fun aSmallElementReserves48dpInBothDirections() {
        rule.setMonarchContent {
            Box(Modifier.testTag("target").monarchMinimumTouchTarget().size(20.dp))
        }
        rule.onNodeWithTag("target").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun aSmallTappableElementReceivesTapsIn48dp() {
        rule.setMonarchContent {
            Box(
                Modifier
                    .monarchMinimumTouchTarget()
                    .size(20.dp)
                    .testTag("target")
                    .clickable { },
            )
        }
        // The visible element stays 20dp; the area that receives taps is 48dp.
        rule.onNodeWithTag("target").assertWidthIsEqualTo(20.dp)
        rule.onNodeWithTag("target").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aLargeElementKeepsItsSize() {
        rule.setMonarchContent {
            Box(Modifier.testTag("target").monarchMinimumTouchTarget().size(width = 120.dp, height = 64.dp))
        }
        rule.onNodeWithTag("target").assertWidthIsEqualTo(120.dp).assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun reducedMotionIsAnAnimatorScaleOfZero() {
        assertTrue(isReducedMotionScale(0f))
        assertFalse(isReducedMotionScale(0.5f))
        assertFalse(isReducedMotionScale(1f))
    }

    @Test
    fun reducedMotionFollowsTheSystemSettingWhileComposed() {
        val context: Context = ApplicationProvider.getApplicationContext()
        var reduced = false
        rule.setContent { reduced = isReducedMotionEnabled() }
        rule.waitForIdle()
        assertFalse(reduced)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        context.contentResolver.notifyChange(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            null,
        )
        rule.waitForIdle()
        assertTrue(reduced)
    }

    @Test
    fun theFocusRingIsDrawnInTheFocusColourOnlyWhileFocused() {
        val requester = FocusRequester()
        rule.setMonarchContent {
            Box(Modifier.testTag("frame").padding(16.dp)) {
                Box(
                    Modifier
                        .testTag("element")
                        .size(40.dp)
                        .monarchFocusRing(MonarchTheme.shapes.none)
                        .focusRequester(requester)
                        .focusable(),
                )
            }
        }
        assertFalse(frameContains(MonarchTokens.light.colors.focusRing), "Ring drawn without focus")
        rule.runOnIdle { requester.requestFocus() }
        rule.onNodeWithTag("element").assertIsFocused()
        assertTrue(frameContains(MonarchTokens.light.colors.focusRing), "Ring not drawn with focus")
    }

    @Test
    fun theFocusRingFollowsTheThemeWhileFocused() {
        val requester = FocusRequester()
        var tokens: MonarchThemeTokens by mutableStateOf(MonarchTokens.light)
        rule.setContent {
            MonarchTheme(tokens = tokens) {
                Box(Modifier.testTag("frame").padding(16.dp)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .monarchFocusRing(MonarchTheme.shapes.none)
                            .focusRequester(requester)
                            .focusable(),
                    )
                }
            }
        }
        rule.runOnIdle { requester.requestFocus() }
        assertTrue(frameContains(MonarchTokens.light.colors.focusRing))
        tokens = MonarchTokens.hcDark
        rule.waitForIdle()
        assertTrue(frameContains(MonarchTokens.hcDark.colors.focusRing), "Ring kept the previous theme")
    }

    /** Whether any pixel of the node tagged "frame" has exactly the colour [argb]. */
    private fun frameContains(argb: Long): Boolean {
        val expected = Color(argb).toArgb()
        val bitmap = rule.onNodeWithTag("frame").captureToImage().asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.any { it == expected }
    }
}
