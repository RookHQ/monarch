@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryEntity
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryEntityKind
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryItem
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryPanel
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryPanelLabels
import com.rookhq.monarch.components.intelligence.aimemorypanel.MonarchAiMemoryPanelTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val Day = 86_400_000L
private const val Now = 1_793_750_400_000L
private val Ada = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Person, "Ada Lovelace")
private val Acme = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Organization, "Acme")
private val Atlas = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Project, "Atlas")

private val Sample = listOf(
    MonarchAiMemoryItem("m1", "Renewal is due in June", Acme, Now - Day),
    MonarchAiMemoryItem("m2", "Prefers short summaries", Ada, Now - 2 * Day),
    MonarchAiMemoryItem("m3", "Works in the Lisbon office", Ada, Now),
)

private val Utc = TimeZone.getTimeZone("UTC")

internal object MonarchAiMemoryPanelFixtures : ComponentFixtures {
    override val id = "ai-memory-panel"
    override val fixtures = listOf(
        fixture("two groups") {
            MonarchAiMemoryPanel(
                memories = Sample.take(2),
                onEdit = { _, _ -> },
                onForget = {},
                onForgetMany = {},
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
        fixture("empty") {
            MonarchAiMemoryPanel(memories = emptyList(), onEdit = { _, _ -> }, onForget = {}, onForgetMany = {})
        },
        fixture("long memory, project") {
            MonarchAiMemoryPanel(
                memories = listOf(
                    MonarchAiMemoryItem(
                        "m9",
                        "Wants every weekly report to start with the three numbers the board asks about",
                        Atlas,
                        Now,
                    ),
                ),
                onEdit = { _, _ -> },
                onForget = {},
                onForgetMany = {},
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMemoryPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun announced(): String? = rule.onAllNodes(live, useUnmergedTree = true).fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.singleOrNull() }
        .lastOrNull()

    private fun panel(
        memories: List<MonarchAiMemoryItem> = Sample,
        onForget: (String) -> Unit = {},
        onForgetMany: (List<String>) -> Unit = {},
        labels: MonarchAiMemoryPanelLabels = MonarchAiMemoryPanelLabels(),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontScale: Float = 1f,
    ) {
        rule.setMonarchContent(layoutDirection = layoutDirection, fontScale = fontScale) {
            MonarchAiMemoryPanel(
                memories = memories,
                onEdit = { _, _ -> },
                onForget = onForget,
                onForgetMany = onForgetMany,
                locale = Locale.US,
                timeZone = Utc.id,
                labels = labels,
                searchIcon = null,
                clearIcon = null,
                editIcon = null,
                forgetIcon = null,
            )
        }
    }

    private fun assertAbove(upper: SemanticsNodeInteraction, lower: SemanticsNodeInteraction) {
        val top = upper.fetchSemanticsNode().boundsInRoot
        val bottom = lower.fetchSemanticsNode().boundsInRoot
        assertTrue(top.bottom <= bottom.top, "$top is not above $bottom")
    }

    @Test
    fun memoriesAreGroupedByEntityPeopleFirst() {
        panel()
        rule.onNodeWithText("Ada Lovelace").assert(isHeading())
        rule.onNodeWithText("Person · 2 memories").assertExists()
        rule.onNodeWithText("Organization · 1 memory").assertExists()
        assertAbove(rule.onNodeWithText("Ada Lovelace"), rule.onNodeWithText("Acme"))
        assertAbove(
            rule.onNodeWithText("Works in the Lisbon office", useUnmergedTree = true),
            rule.onNodeWithText("Prefers short summaries", useUnmergedTree = true),
        )
    }

    @Test
    fun selectingAndForgettingSeveralAsksFirst() {
        var forgotten: List<String>? = null
        panel(onForgetMany = { forgotten = it })
        rule.onNodeWithContentDescription("Select all memories about Ada Lovelace")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Select all (3)")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        rule.onNode(hasText("Forget") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        rule.onNodeWithText("Forget 2 memories?").assertExists()
        rule.onNode(hasText("Forget") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(setOf("m2", "m3"), forgotten?.toSet())
        assertEquals("2 memories forgotten", announced())
    }

    @Test
    fun cancelKeepsTheMemories() {
        var calls = 0
        panel(onForgetMany = { calls++ })
        rule.onNodeWithContentDescription("Select memory: Renewal is due in June").performClick()
        rule.onNode(hasText("Forget") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Forget 1 memory?").assertDoesNotExist()
        assertEquals(0, calls)
    }

    @Test
    fun selectAllSelectsEveryShownMemory() {
        panel()
        rule.onNodeWithText("Select all (3)")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.onNodeWithContentDescription("Select memory: Prefers short summaries")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun searchFiltersByTextAndEntityAndAnnouncesMatches() {
        panel()
        rule.onNode(hasSetTextAction()).performTextInput("acme")
        rule.mainClock.advanceTimeBy(MonarchAiMemoryPanelTokens.SEARCH_DEBOUNCE_MILLIS * 2)
        rule.onNodeWithText("Renewal is due in June", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Ada Lovelace").assertDoesNotExist()
        rule.onNodeWithText("Select all (1)").assertExists()
        assertEquals("1 memory matches", announced())
    }

    @Test
    fun aSearchWithoutMatchesSaysSo() {
        panel()
        rule.onNode(hasSetTextAction()).performTextInput("zebra")
        rule.onNodeWithText("No memories match “zebra”.").assertExists()
    }

    @Test
    fun forgettingOneRowCallsOnForgetAndAnnounces() {
        var forgotten = ""
        panel(onForget = { forgotten = it })
        val row = rule.onNode(hasText("Renewal is due in June") and hasClickAction())
        val forget = row.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "Forget" }
        rule.runOnIdle { forget.action() }
        assertEquals("m1", forgotten)
        assertEquals("Memory forgotten", announced())
    }

    @Test
    fun theUpdatedDateIsShownInTheLocale() {
        panel(memories = listOf(MonarchAiMemoryItem("m1", "Text", Ada, Now)))
        rule.onNodeWithText("Updated Nov 4, 2026").assertExists()
    }

    @Test
    fun nothingRememberedShowsTheEmptyText() {
        panel(memories = emptyList())
        rule.onNodeWithText("Nothing is remembered yet.").assertExists()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        panel(
            labels = MonarchAiMemoryPanelLabels(
                selectAll = { "Tout sélectionner ($it)" },
                kind = { if (it == MonarchAiMemoryEntityKind.Person) "Personne" else "Autre" },
                count = { "$it souvenirs" },
                select = { "Choisir : $it" },
            ),
        )
        rule.onNodeWithText("Tout sélectionner (3)").assertExists()
        rule.onNodeWithText("Personne · 2 souvenirs").assertExists()
        rule.onNode(hasContentDescription("Choisir : Prefers short summaries")).assertExists()
    }

    @Test
    fun theCheckboxIsAtTheStartInRightToLeft() {
        panel(memories = Sample.take(1), layoutDirection = LayoutDirection.Rtl)
        rule.onNodeWithContentDescription("Select memory: Renewal is due in June")
            .assertIsBefore(rule.onNodeWithText("Renewal is due in June", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun aMemoryWrapsAtTheLargestFontSize() {
        panel(memories = Sample.take(1), fontScale = LargestFontScale)
        rule.onNodeWithText("Renewal is due in June", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicGroupsMatchesAndSelects() {
        val t = MonarchAiMemoryPanelTokens
        val groups = t.groups(Sample + MonarchAiMemoryItem("m4", "Ships in May", Atlas, Now), "", Locale.US)
        assertEquals(listOf("Ada Lovelace", "Acme", "Atlas"), groups.map { it.entity.name })
        assertEquals(listOf("m3", "m2"), groups[0].ids)
        assertEquals(listOf("m1"), t.groups(Sample, "  ACME ", Locale.US).flatMap { it.ids })
        assertEquals(ToggleableState.Off, t.checkState(emptyList(), listOf("a", "b")))
        assertEquals(ToggleableState.Indeterminate, t.checkState(listOf("a"), listOf("a", "b")))
        assertEquals(ToggleableState.On, t.checkState(listOf("a", "b"), listOf("a", "b")))
        val some = ToggleableState.Indeterminate
        assertEquals(listOf("x", "a", "b"), t.toggled(listOf("x", "a"), listOf("a", "b"), some))
        assertEquals(listOf("x"), t.toggled(listOf("x", "a", "b"), listOf("a", "b"), ToggleableState.On))
        assertEquals("Short", t.shortText("  Short  "))
        val long = t.shortText("word ".repeat(20))
        assertEquals(t.SHORT_TEXT, long.length)
        assertTrue(long.endsWith("…"))
    }

    @Test
    fun memoryEventsUpdateTheList() {
        val write = MonarchAiMemoryPart("m1", MonarchAiMemoryAction.Write, "Renewal moved to July")
        val updated = MonarchAiMemoryItem.apply(Sample, write, now = Now)
        assertEquals("Renewal moved to July" to Now, updated[0].text to updated[0].updatedAt)
        assertSame(Sample, MonarchAiMemoryItem.apply(Sample, write.copy(text = Sample[0].text), now = Now))
        assertSame(Sample, MonarchAiMemoryItem.apply(Sample, write.copy(action = MonarchAiMemoryAction.Read), Now))
        val removed = MonarchAiMemoryItem.apply(Sample, write.copy(action = MonarchAiMemoryAction.Forget), Now)
        assertFalse(removed.any { it.id == "m1" })
        val fresh = MonarchAiMemoryPart("m9", MonarchAiMemoryAction.Write, "New")
        val added = MonarchAiMemoryItem.apply(Sample, fresh, Now)
        assertEquals(MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Other, "General"), added.last().entity)
        val scoped = MonarchAiMemoryPart("m8", MonarchAiMemoryAction.Write, "New", scope = "Atlas")
        assertEquals("Atlas", MonarchAiMemoryItem.apply(Sample, scoped, Now).last().entity.name)
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val colors = MonarchAiMemoryPanelTokens.colors(c)
            assertEquals(c.textDefault, colors.heading, theme.id)
            assertEquals(c.textMuted, colors.muted, theme.id)
            assertEquals(c.borderSubtle, colors.divider, theme.id)
        }
    }

    @Test
    fun anAnnouncementIsNotMadeOnFirstShow() {
        var memories by mutableStateOf(Sample)
        rule.setMonarchContent {
            MonarchAiMemoryPanel(memories = memories, onEdit = { _, _ -> }, onForget = {}, onForgetMany = {})
        }
        assertEquals(null, announced())
        memories = Sample.take(1)
        rule.onNodeWithText("Select all (1)").assertExists()
    }
}
