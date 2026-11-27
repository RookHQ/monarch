package com.rookhq.monarch.components.forms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControlTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object SegmentedControlFixtures : ComponentFixtures {
    override val id = "segmented-control"
    override val fixtures = listOf(
        fixture("two options") { MonarchSegmentedControl(listOf("List", "Board"), selectedIndex = 0, onSelect = {}) },
        fixture("three options, last selected") {
            MonarchSegmentedControl(listOf("Day", "Week", "Month"), selectedIndex = 2, onSelect = {})
        },
        fixture("one option disabled, full width") {
            MonarchSegmentedControl(
                listOf("Open", "Closed", "All"),
                selectedIndex = 0,
                onSelect = {},
                fillWidth = true,
                disabledIndices = setOf(1),
            )
        },
        fixture("icons") {
            MonarchSegmentedControl {
                Segment(
                    selected = true,
                    onClick = {},
                    label = "List",
                    icon = { MonarchIcon(SampleIcons.ListSimple, null, size = MonarchIconSize.Sm) },
                )
                Segment(
                    selected = false,
                    onClick = {},
                    label = "Board",
                    icon = { MonarchIcon(SampleIcons.Board, null, size = MonarchIconSize.Sm) },
                )
                Segment(
                    selected = false,
                    onClick = {},
                    label = "Grid",
                    icon = { MonarchIcon(SampleIcons.Grid, null, size = MonarchIconSize.Sm) },
                )
            }
        },
        fixture("counts and a suffix") {
            MonarchSegmentedControl {
                Segment(selected = false, onClick = {}, label = "Open", count = 12)
                Segment(selected = true, onClick = {}, label = "Done", count = 3)
                Segment(selected = false, onClick = {}, label = "Yearly", suffix = "Billed yearly")
            }
        },
        fixture("small and large") {
            androidx.compose.foundation.layout.Column {
                MonarchSegmentedControl(
                    listOf("1 day", "7 days", "30 days"),
                    selectedIndex = 1,
                    onSelect = {},
                    size = MonarchSegmentedControlSize.Sm,
                )
                MonarchSegmentedControl(
                    listOf("Monthly", "Yearly"),
                    selectedIndex = 0,
                    onSelect = {},
                    size = MonarchSegmentedControlSize.Lg,
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class SegmentedControlTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachSegmentIsARadioButtonAndOneIsSelected() {
        rule.setMonarchContent {
            var selected by remember { mutableIntStateOf(0) }
            MonarchSegmentedControl(
                listOf("Day", "Week", "Month"),
                selectedIndex = selected,
                onSelect = { selected = it },
            )
        }
        rule.onNodeWithText("Day")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        rule.onNodeWithText("Week").assertIsNotSelected().performClick()
        rule.onNodeWithText("Week").assertIsSelected()
        rule.onNodeWithText("Day").assertIsNotSelected()
    }

    @Test
    fun aDisabledSegmentCannotBeChosen() {
        var chosen = -1
        rule.setMonarchContent {
            MonarchSegmentedControl(
                listOf("Open", "Closed"),
                selectedIndex = 0,
                onSelect = { chosen = it },
                disabledIndices = setOf(1),
            )
        }
        rule.onNodeWithText("Closed").assertIsNotEnabled().performClick()
        assertEquals(-1, chosen)
    }

    @Test
    fun segmentsHave48dpTargetsAndFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSegmentedControl(listOf("List", "Board"), selectedIndex = 0, onSelect = {})
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("List").assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("List", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Board", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun labelsAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSegmentedControl(listOf("List", "Board"), selectedIndex = 0, onSelect = {})
        }
        rule.onNodeWithText("Board", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theTrackIsMutedAndTheChosenPillIsTheSurface() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgMuted, MonarchSegmentedControlTokens.track(c), theme.id)
            // Light: the surface with a hairline. Dark: one surface step up, no border, no shadow.
            val pill = if (theme.isDark) c.bgEmphasis else c.bgSurface
            assertEquals(pill, MonarchSegmentedControlTokens.indicator(c, theme.isDark), theme.id)
            val hairline = if (theme.isDark) Color.Transparent else c.borderDefault
            assertEquals(hairline, MonarchSegmentedControlTokens.indicatorBorder(c, theme.isDark), theme.id)
            assertEquals(c.textDefault, MonarchSegmentedControlTokens.content(true, true, false, c), theme.id)
            assertEquals(c.textMuted, MonarchSegmentedControlTokens.content(false, true, false, c), theme.id)
            assertEquals(c.textDisabled, MonarchSegmentedControlTokens.content(true, false, false, c), theme.id)
        }
    }

    @Test
    fun theVisibleHeightsAreTheWebControlHeights() {
        assertEquals(32f, MonarchSegmentedControlTokens.trackHeight(MonarchSegmentedControlSize.Sm).value)
        assertEquals(36f, MonarchSegmentedControlTokens.trackHeight(MonarchSegmentedControlSize.Md).value)
        assertEquals(32f, MonarchSegmentedControlTokens.pillHeight(MonarchSegmentedControlSize.Md).value)
    }

    @Test
    fun choosingAnotherSegmentTicksOnceAndTheChosenOneDoesNot() {
        val ticks = mutableListOf<HapticFeedbackType>()
        val haptics = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                ticks += hapticFeedbackType
            }
        }
        rule.setMonarchContent {
            var chosen by remember { mutableIntStateOf(0) }
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                MonarchSegmentedControl(listOf("List", "Board"), selectedIndex = chosen, onSelect = { chosen = it })
            }
        }
        rule.onNodeWithText("List").performClick()
        rule.onNodeWithText("Board").performClick()
        assertEquals(listOf(HapticFeedbackType.SegmentTick), ticks)
    }
}
