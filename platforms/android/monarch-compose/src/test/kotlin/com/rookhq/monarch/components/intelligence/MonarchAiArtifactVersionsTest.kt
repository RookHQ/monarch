@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersion
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersions
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersionsLabels
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersionsTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 2026-11-03 14:00 UTC.
private const val Start = 1_793_714_400_000L
private const val Hour = 3_600_000L
private val Utc = TimeZone.getTimeZone("UTC")
private val Three = listOf(
    MonarchAiArtifactVersion(1, Start, content = "Ship on Friday.\nTell support."),
    MonarchAiArtifactVersion(2, Start + Hour, label = "Shorter intro", content = "Ship on Monday.\nTell support."),
    MonarchAiArtifactVersion(3, Start + 2 * Hour, content = "Ship on Monday.\nTell everyone."),
)

@Composable
private fun Stepper(
    selection: Int,
    versions: List<MonarchAiArtifactVersion> = Three,
    onSelectionChange: (Int) -> Unit = {},
    comparing: Boolean? = null,
    enabled: Boolean = true,
    labels: MonarchAiArtifactVersionsLabels = MonarchAiArtifactVersionsLabels(),
) {
    MonarchAiArtifactVersions(
        selection = selection,
        versions = versions,
        onSelectionChange = onSelectionChange,
        previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        comparing = comparing,
        enabled = enabled,
        locale = Locale.US,
        timeZone = Utc.id,
        labels = labels,
    )
}

internal object MonarchAiArtifactVersionsFixtures : ComponentFixtures {
    override val id = "ai-artifact-versions"
    override val fixtures = listOf(
        fixture("middle version") { Stepper(selection = 2) },
        fixture("first version") { Stepper(selection = 1) },
        fixture("single version") { Stepper(selection = 1, versions = Three.take(1)) },
        fixture("comparing") { Stepper(selection = 3, comparing = true) },
        fixture("no content, no compare") {
            Stepper(selection = 2, versions = Three.map { it.copy(content = null) })
        },
        fixture("disabled") { Stepper(selection = 2, enabled = false) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiArtifactVersionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theControlsAreAGroupWithAPositionAndNamedButtons() {
        rule.setMonarchContent { Stepper(selection = 2) }
        rule.onNodeWithContentDescription("Versions").assertExists()
        rule.onNodeWithText("Version 2 of 3").assertExists()
        rule.onNodeWithContentDescription("Previous version").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Next version").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun previousAndNextChangeTheVersionAndStopAtTheEnds() {
        var selection by mutableIntStateOf(2)
        rule.setMonarchContent { Stepper(selection = selection, onSelectionChange = { selection = it }) }
        rule.onNodeWithContentDescription("Next version").performClick()
        assertEquals(3, selection)
        rule.onNodeWithContentDescription("Next version").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Previous version").performClick()
        rule.onNodeWithContentDescription("Previous version").performClick()
        assertEquals(1, selection)
        rule.onNodeWithContentDescription("Previous version").assertIsNotEnabled()
    }

    @Test
    fun theMenuJumpsToAVersionMarksTheShownOneAndAnnounces() {
        var selection by mutableIntStateOf(1)
        rule.setMonarchContent { Stepper(selection = selection, onSelectionChange = { selection = it }) }
        // The button shows the shown version (as the web select), so the menu rows are the ones with a selected state.
        val row = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        rule.onNode(hasText("v1 · Nov 3, 2026", substring = true) and hasClickAction()).performClick()
        rule.onNode(hasText("v1 · Nov 3, 2026", substring = true) and row).assertIsSelected()
        rule.onNode(hasText("v2 · Shorter intro · Nov 3, 2026", substring = true) and row)
            .assertIsNotSelected()
            .performClick()
        assertEquals(2, selection)
        rule.waitForIdle()
        val spoken = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.single()
        assertEquals("Showing version 2 of 3", spoken)
    }

    @Test
    fun nothingIsAnnouncedAtFirst() {
        rule.setMonarchContent { Stepper(selection = 2) }
        val spoken = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)
        assertNull(spoken)
    }

    @Test
    fun compareShowsTheDiffFromThePreviousVersion() {
        rule.setMonarchContent { Stepper(selection = 2) }
        val compare = rule.onNode(hasText("Compare with previous") and hasClickAction())
        compare.assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density)
        compare.performClick()
        compare.assertIsSelected()
        rule.onAllNodes(hasContentDescription("Version 1", substring = true)).fetchSemanticsNodes().let {
            assertTrue(it.isNotEmpty(), "the diff names the earlier version")
        }
        rule.onNode(hasText("Monday", substring = true), useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAppCanOwnTheCompareState() {
        val asked = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchAiArtifactVersions(
                selection = 2,
                versions = Three,
                onSelectionChange = {},
                previousIcon = {},
                nextIcon = {},
                comparing = false,
                onComparingChange = { asked += it },
            )
        }
        rule.onNode(hasText("Compare with previous") and hasClickAction()).performClick()
        assertEquals(listOf(true), asked)
        rule.onNode(hasText("Compare with previous") and hasClickAction()).assertIsNotSelected()
    }

    @Test
    fun theFirstVersionAndVersionsWithoutContentHaveNoCompare() {
        rule.setMonarchContent { Stepper(selection = 1) }
        rule.onNode(hasText("Compare with previous")).assertDoesNotExist()
    }

    @Test
    fun aSingleVersionShowsItsPositionWithBothButtonsDisabled() {
        rule.setMonarchContent { Stepper(selection = 1, versions = Three.take(1)) }
        rule.onNodeWithText("Version 1 of 1").assertExists()
        rule.onNodeWithContentDescription("Previous version").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next version").assertIsNotEnabled()
    }

    @Test
    fun noVersionsShowNothing() {
        rule.setMonarchContent { Stepper(selection = 1, versions = emptyList()) }
        rule.onNodeWithContentDescription("Versions").assertDoesNotExist()
    }

    @Test
    fun disabledDisablesEveryControl() {
        rule.setMonarchContent { Stepper(selection = 2, enabled = false) }
        rule.onNodeWithContentDescription("Previous version").assertIsNotEnabled()
        rule.onNode(hasText("v2 · Shorter intro", substring = true) and hasClickAction()).assertIsNotEnabled()
        rule.onNode(hasText("Compare with previous") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun anUnknownSelectionShowsTheNewest() {
        rule.setMonarchContent { Stepper(selection = 9) }
        rule.onNodeWithText("Version 3 of 3").assertExists()
        rule.onNodeWithContentDescription("Next version").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Previous version").assertIsEnabled()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            Stepper(
                selection = 2,
                labels = MonarchAiArtifactVersionsLabels(
                    label = "Fassungen",
                    position = { v, t -> "Fassung $v von $t" },
                    previous = "Vorherige",
                    select = "Fassung wählen",
                    compare = "Vergleichen",
                ),
            )
        }
        rule.onNodeWithContentDescription("Fassungen").assertExists()
        rule.onNodeWithText("Fassung 2 von 3").assertExists()
        rule.onNodeWithContentDescription("Vorherige").assertExists()
        rule.onNodeWithText("Fassung wählen").assertExists()
        rule.onNodeWithText("Vergleichen").assertExists()
    }

    @Test
    fun previousComesFirstInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Stepper(selection = 2) }
        rule.onNodeWithContentDescription("Previous version")
            .assertIsBefore(rule.onNodeWithContentDescription("Next version"), LayoutDirection.Rtl)
    }

    @Test
    fun theControlsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Stepper(selection = 2) }
        rule.onNodeWithText("Compare with previous").assertTextFullyVisible()
        rule.onNodeWithText("v2 · Shorter intro", substring = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicSortsPlacesAndFormats() {
        val t = MonarchAiArtifactVersionsTokens
        val shuffled = listOf(Three[2], Three[0], Three[1])
        val place = t.place(shuffled, 2)
        assertEquals(listOf(1, 2, 3), place.sorted.map { it.version })
        assertEquals(1, place.index)
        assertEquals(1, place.previous?.version)
        assertEquals(2, t.place(shuffled, 7).index)
        assertEquals(-1, t.place(emptyList(), 1).index)
        val duplicate = t.place(Three + MonarchAiArtifactVersion(2, Start, label = "Again"), 2)
        assertEquals(3, duplicate.sorted.size)
        assertEquals("Again", duplicate.current?.label)
        assertTrue(t.canCompare(place, showCompare = true))
        assertFalse(t.canCompare(place, showCompare = false))
        assertFalse(t.canCompare(t.place(Three, 1), showCompare = true))
        assertEquals("v2 · Shorter intro · date", t.option("v2", "Shorter intro", "date"))
        assertEquals("v2", t.option("v2", " ", ""))
        assertEquals("Nov 3, 2026, 2:00 PM", t.date(Start, Locale.US, Utc).replace('\u202F', ' '))
    }
}
