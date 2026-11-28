package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.layout.deviceframe.MonarchDeviceFrame
import com.rookhq.monarch.components.layout.deviceframe.MonarchDeviceFrameDevice
import com.rookhq.monarch.components.layout.deviceframe.MonarchDeviceFrameTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchDeviceFrameFixtures : ComponentFixtures {
    override val id = "device-frame"
    override val fixtures = MonarchDeviceFrameDevice.entries.flatMap { device ->
        listOf(
            fixture("${device.name}, preview") {
                MonarchDeviceFrame(
                    label = "Preview of your link page",
                    device = device,
                    screenSize = DpSize(160.dp, 300.dp),
                ) {
                    MonarchText(text = "Ada Lovelace", modifier = Modifier.padding(8.dp))
                }
            },
            fixture("${device.name}, flat with status bar") {
                MonarchDeviceFrame(
                    label = "Preview",
                    device = device,
                    screenSize = DpSize(200.dp, 300.dp),
                    statusBarTime = "9:41",
                    flat = true,
                ) {
                    MonarchText(text = "Links", modifier = Modifier.padding(8.dp))
                }
            },
        )
    } + fixture("interactive") {
        MonarchDeviceFrame(label = "Live preview", screenSize = DpSize(200.dp, 300.dp), interactive = true) {
            Column(modifier = Modifier.padding(16.dp)) {
                MonarchButton(text = "Subscribe", onClick = {})
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchDeviceFrameTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aPreviewIsOneImageNamedByItsLabelAndItsContentTakesNoTouches() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchDeviceFrame(label = "Preview of your page", screenSize = DpSize(200.dp, 400.dp)) {
                MonarchButton(text = "Subscribe", onClick = { clicks++ })
            }
        }
        val frame = rule.onNodeWithContentDescription("Preview of your page")
        frame.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        rule.onAllNodesWithText("Subscribe").assertCountEquals(0)
        frame.performTouchInput { click(center) }
        frame.performTouchInput { click(topCenter.copy(y = centerY / 4)) }
        assertEquals(0, clicks)
    }

    @Test
    fun anInteractiveFrameKeepsItsContentUsable() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchDeviceFrame(label = "Live preview", screenSize = DpSize(200.dp, 400.dp), interactive = true) {
                MonarchButton(text = "Subscribe", onClick = { clicks++ })
            }
        }
        rule.onNodeWithContentDescription("Live preview")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        rule.onNodeWithText("Subscribe").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun theDecorationIsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchDeviceFrame(
                label = "Live preview",
                screenSize = DpSize(200.dp, 400.dp),
                statusBarTime = "9:41",
                interactive = true,
            ) {
                MonarchText(text = "Content")
            }
        }
        rule.onAllNodesWithText("9:41").assertCountEquals(0)
        rule.onNodeWithText("Content").assertExists()
    }

    @Test
    fun theFrameNeverGrowsPastTheScreenWidthAndKeepsTheAspectRatio() {
        rule.setMonarchContent {
            MonarchDeviceFrame(label = "Frame", screenSize = DpSize(200.dp, 300.dp)) {}
        }
        rule.onNodeWithContentDescription("Frame").assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(300.dp)
    }

    @Test
    fun theFrameScalesDownToANarrowerContainer() {
        // The test window is 320dp wide; the default phone is 390 by 844.
        rule.setMonarchContent { MonarchDeviceFrame(label = "Frame") {} }
        val bounds = rule.onNodeWithContentDescription("Frame").fetchSemanticsNode().size
        val ratio = bounds.height.toFloat() / bounds.width
        assertEquals(844f / 390f, ratio, 0.02f)
    }

    @Test
    fun onlyAPhoneHasAnIslandAndVolumeButtons() {
        assertTrue(MonarchDeviceFrameTokens.drawsIsland(MonarchDeviceFrameDevice.Phone))
        assertFalse(MonarchDeviceFrameTokens.drawsIsland(MonarchDeviceFrameDevice.Tablet))
        assertTrue(MonarchDeviceFrameTokens.drawsStartButtons(MonarchDeviceFrameDevice.Phone))
        assertFalse(MonarchDeviceFrameTokens.drawsStartButtons(MonarchDeviceFrameDevice.Tablet))
        val phone = MonarchDeviceFrameTokens.geometry(MonarchDeviceFrameDevice.Phone)
        val tablet = MonarchDeviceFrameTokens.geometry(MonarchDeviceFrameDevice.Tablet)
        assertEquals(DpSize(390.dp, 844.dp), phone.screen)
        assertEquals(DpSize(820.dp, 1180.dp), tablet.screen)
        assertEquals(34.dp, phone.screenCorner)
        assertEquals(14.dp, tablet.screenCorner)
    }

    @Test
    fun theFrameIsADarkNeutralAroundTheSurfaceInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val frame = MonarchDeviceFrameTokens.colors(colors, theme.isDark)
            assertEquals(if (theme.isDark) colors.bgEmphasis else colors.bgInverse, frame.frame, theme.id)
            assertEquals(colors.bgSurface, frame.screen, theme.id)
            assertEquals(colors.textDefault, frame.content, theme.id)
        }
    }
}
