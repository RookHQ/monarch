package com.rookhq.monarch.components.collaboration

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControl
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControls
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLabels
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLayout
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val glyph: @Composable (MonarchCallControl, Boolean) -> Unit = { _, _ ->
    Icon(TestIcon, contentDescription = null)
}

internal object MonarchCallControlsFixtures : ComponentFixtures {
    override val id = "call-controls"
    override val fixtures = listOf(
        fixture("every control, devices on") {
            MonarchCallControls(
                icon = glyph,
                onMicOnChange = {},
                onCameraOnChange = {},
                onSharingChange = {},
                onHandRaisedChange = {},
                onReact = {},
                onLayoutChange = {},
                onLeave = {},
            )
        },
        fixture("muted, camera off, sharing, hand raised") {
            MonarchCallControls(
                icon = glyph,
                micOn = false,
                onMicOnChange = {},
                cameraOn = false,
                onCameraOnChange = {},
                sharing = true,
                onSharingChange = {},
                handRaised = true,
                onHandRaisedChange = {},
            )
        },
        fixture("minimal") {
            MonarchCallControls(icon = glyph, onMicOnChange = {}, onCameraOnChange = {}, minimal = true, onLeave = {})
        },
        fixture("disabled") {
            MonarchCallControls(icon = glyph, onMicOnChange = {}, onReact = {}, enabled = false, onLeave = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCallControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachToggleKeepsItsNameAndReportsOnOrOff() {
        var mic by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchCallControls(
                icon = glyph,
                micOn = mic,
                onMicOnChange = { mic = it },
                cameraOn = false,
                onCameraOnChange = {},
            )
        }
        val microphone = rule.onNodeWithContentDescription("Microphone")
        microphone.assertIsOn().assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("On", microphone.stateDescription())
        microphone.performClick()
        assertFalse(mic)
        microphone.assertIsOff()
        assertEquals("Off", microphone.stateDescription())
        rule.onNodeWithContentDescription("Camera").assertIsOff()
    }

    @Test
    fun theGroupIsNamedAndOnlyControlsWithACallbackAreShown() {
        rule.setMonarchContent { MonarchCallControls(icon = glyph, onMicOnChange = {}) }
        rule.onNodeWithContentDescription("Call controls").assertExists()
        rule.onNodeWithContentDescription("Camera").assertDoesNotExist()
        rule.onNodeWithContentDescription("More options").assertDoesNotExist()
        rule.onNodeWithContentDescription("Leave call").assertDoesNotExist()
    }

    @Test
    fun aReactionIsSentFromTheMenu() {
        val sent = mutableListOf<String>()
        rule.setMonarchContent { MonarchCallControls(icon = glyph, onReact = { sent += it }) }
        rule.onNodeWithContentDescription("Send a reaction").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("React 👍").performClick()
        assertEquals(listOf("👍"), sent)
        rule.onNodeWithContentDescription("React 👍").assertDoesNotExist()
    }

    @Test
    fun moreOffersTheLayoutLowDataAndTheAppsScreens() {
        var layout by mutableStateOf(MonarchCallControlsLayout.Automatic)
        var lowData by mutableStateOf(false)
        var devices = 0
        rule.setMonarchContent {
            MonarchCallControls(
                icon = glyph,
                layout = layout,
                onLayoutChange = { layout = it },
                lowData = lowData,
                onLowDataChange = { lowData = it },
                onOpenDevices = { devices++ },
                onOpenStats = {},
                onPictureInPicture = {},
            )
        }
        val more = rule.onNodeWithContentDescription("More options")
        more.performClick()
        rule.onNodeWithText("Automatic").assertIsSelected()
        rule.onNodeWithText("Grid").assertIsNotSelected().performClick()
        assertEquals(MonarchCallControlsLayout.Grid, layout)
        more.performClick()
        rule.onNodeWithText("Low data mode (audio only)").assertIsOff().performClick()
        assertTrue(lowData)
        rule.onNodeWithText("Low data mode (audio only)").assertIsOn()
        rule.onNodeWithText("Connection and data use").assertExists()
        rule.onNodeWithText("Picture in picture").assertExists()
        rule.onNodeWithText("Devices").performClick()
        assertEquals(1, devices)
    }

    @Test
    fun theMinimalFormLeavesOutTheHandReactionsAndMore() {
        rule.setMonarchContent {
            MonarchCallControls(
                icon = glyph,
                onMicOnChange = {},
                onHandRaisedChange = {},
                onReact = {},
                onOpenDevices = {},
                minimal = true,
                onLeave = {},
            )
        }
        rule.onNodeWithContentDescription("Microphone").assertExists()
        rule.onNodeWithContentDescription("Raise hand").assertDoesNotExist()
        rule.onNodeWithContentDescription("Send a reaction").assertDoesNotExist()
        rule.onNodeWithContentDescription("More options").assertDoesNotExist()
        rule.onNodeWithContentDescription("Leave call").assertExists()
    }

    @Test
    fun leaveCallsBackAndEverythingIsOffWhenDisabled() {
        var left = 0
        var enabled by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchCallControls(icon = glyph, onMicOnChange = {}, enabled = enabled, onLeave = { left++ })
        }
        rule.onNodeWithContentDescription("Leave call").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, left)
        enabled = false
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Microphone").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Leave call").assertIsNotEnabled()
    }

    @Test
    fun theControlsKeepTheirOrderInBothDirectionsAndWrapAtTheLargestFontSize() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchCallControls(icon = glyph, onMicOnChange = {}, onCameraOnChange = {}, onLeave = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Microphone")
                .assertIsBefore(rule.onNodeWithContentDescription("Camera"), direction)
            rule.onNodeWithContentDescription("Leave call").assertIsDisplayed()
        }
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchCallControls(
                icon = glyph,
                micOn = false,
                onMicOnChange = {},
                onLeave = {},
                labels = MonarchCallControlsLabels(microphone = "Mic", off = "Muted", leave = "Hang up"),
            )
        }
        assertEquals("Muted", rule.onNodeWithContentDescription("Mic").stateDescription())
        rule.onNodeWithContentDescription("Hang up").assertExists()
    }

    @Test
    fun theRulesOfTheRow() {
        val t = MonarchCallControlsTokens
        assertFalse(t.showsMore(minimal = true, itemCount = 5))
        assertFalse(t.showsMore(minimal = false, itemCount = 0))
        assertTrue(t.showsMore(minimal = false, itemCount = 1))
        assertFalse(t.showsReactions(minimal = false, reactions = emptyList(), hasAction = true))
        assertFalse(t.showsHand(minimal = true, hasAction = true))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchCallControlsTokens
            val mic = MonarchCallControl.Microphone
            assertEquals(colors.bgMuted, t.colors(mic, on = true, enabled = true, colors = colors).container, theme.id)
            val muted = t.colors(mic, on = false, enabled = true, colors = colors)
            assertEquals(colors.dangerSolid, muted.container, theme.id)
            assertEquals(colors.dangerOnSolid, muted.content, theme.id)
            val share = t.colors(MonarchCallControl.ShareScreen, on = true, enabled = true, colors = colors)
            assertEquals(colors.successSolid, share.container, theme.id)
            val hand = t.colors(MonarchCallControl.RaiseHand, on = true, enabled = true, colors = colors)
            assertEquals(colors.warningSolid, hand.container, theme.id)
            val off = t.colors(MonarchCallControl.Camera, on = false, enabled = false, colors = colors)
            assertEquals(colors.textDisabled, off.content, theme.id)
        }
    }
}
