@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCard
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCardBody
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCardLabels
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCardStatus
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCardTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_793_750_400_000L
private const val Hour = 3_600_000L
private val Ticket = MonarchAiSourcePart("t1", "Ticket 4821", null, MonarchAiSourceKind.Record)

internal object MonarchAiSummaryCardFixtures : ComponentFixtures {
    override val id = "ai-summary-card"
    override val fixtures = listOf(
        fixture("ready, with age and refresh") {
            MonarchAiSummaryCard(
                title = "Account summary",
                summary = "**Acme** renewed in March.",
                generatedAt = Now - 3 * Hour,
                now = Now,
                onRefresh = {},
            )
        },
        fixture("with a source") {
            MonarchAiSummaryCard(title = "Account", summary = "Two open tickets.", sources = listOf(Ticket))
        },
        fixture("generating, no summary yet") {
            MonarchAiSummaryCard(title = "Deal", summary = "", status = MonarchAiSummaryCardStatus.Generating)
        },
        fixture("refreshing a summary") {
            MonarchAiSummaryCard(title = "Deal", summary = "Closing in May.", refreshing = true, onRefresh = {})
        },
        fixture("stale") {
            MonarchAiSummaryCard(title = "Deal", summary = "Closing in May.", status = MonarchAiSummaryCardStatus.Stale)
        },
        fixture("error with detail") {
            MonarchAiSummaryCard(
                title = "Deal",
                summary = "",
                status = MonarchAiSummaryCardStatus.Error,
                error = "The model did not answer.",
                onRefresh = {},
            )
        },
        fixture("empty") { MonarchAiSummaryCard(title = "Deal", summary = "") },
        fixture("long title") {
            MonarchAiSummaryCard(
                title = "Summary of the renewal negotiation with the purchasing team at Acme Corporation",
                summary = "Short.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSummaryCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCardIsMarkedAsAnAiSummaryWithAHeadingAndTheSummary() {
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Account summary", summary = "**Acme** renewed in March.")
        }
        rule.onNodeWithText("AI summary", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Account summary").assert(isHeading())
        rule.onNodeWithText("Acme renewed in March.").assertExists()
    }

    @Test
    fun refreshAsksForANewSummary() {
        var refreshed = 0
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Account", summary = "Text.", onRefresh = { refreshed++ })
        }
        rule.onNode(hasText("Refresh") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .assertHasClickAction()
            .performClick()
        assertEquals(1, refreshed)
    }

    @Test
    fun withoutASummaryAPlaceholderShowsWhileGenerating() {
        // The placeholder shimmers for ever, so the clock is advanced by hand.
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Deal", summary = "", status = MonarchAiSummaryCardStatus.Generating)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Generating summary…").assertExists()
        rule.onNodeWithText("No summary yet.").assertDoesNotExist()
    }

    @Test
    fun withoutASummaryTheCardSaysSoUnlessItFailed() {
        var status by mutableStateOf(MonarchAiSummaryCardStatus.Ready)
        rule.setMonarchContent { MonarchAiSummaryCard(title = "Deal", summary = "", status = status) }
        rule.onNodeWithText("No summary yet.").assertExists()
        status = MonarchAiSummaryCardStatus.Error
        rule.waitForIdle()
        rule.onNodeWithText("No summary yet.").assertDoesNotExist()
        rule.onNodeWithText("The summary couldn’t be generated.", substring = true).assertExists()
    }

    @Test
    fun anErrorShowsItsDetailAndAStaleSummaryWarns() {
        rule.setMonarchContent {
            MonarchAiSummaryCard(
                title = "Deal",
                summary = "",
                status = MonarchAiSummaryCardStatus.Error,
                error = "The model did not answer.",
            )
        }
        rule.onNodeWithText("The model did not answer.", substring = true).assertExists()
        rule.onNodeWithText("The summary couldn’t be generated.", substring = true).assertExists()
    }

    @Test
    fun aStaleSummaryKeepsTheSummaryAndWarns() {
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Deal", summary = "Closing in May.", status = MonarchAiSummaryCardStatus.Stale)
        }
        rule.onNodeWithText("Closing in May.").assertExists()
        rule.onNodeWithText("The data changed since this summary was written", substring = true).assertExists()
    }

    @Test
    fun theAgeIsRelativeAndReadAsADate() {
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Deal", summary = "Text.", generatedAt = Now - 3 * Hour, now = Now)
        }
        val age = rule.onNodeWithText("Generated 3 hours ago")
        val spoken = age.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertTrue(spoken.startsWith("Generated "))
        assertFalse(spoken.contains("ago"))
    }

    @Test
    fun sourcesAreListedAndOpen() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiSummaryCard(
                title = "Account",
                summary = "Text.",
                sources = listOf(Ticket),
                onOpenSource = { opened += it.id },
            )
        }
        rule.onNodeWithText("Sources").assertExists()
        rule.onNode(hasContentDescription("Ticket 4821", substring = true) and hasClickAction()).performClick()
        assertEquals(listOf("t1"), opened)
    }

    @Test
    fun theStartAndTheEndOfTheWorkAreAnnouncedPolitely() {
        var refreshing by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiSummaryCard(title = "Deal", summary = "Text.", refreshing = refreshing, onRefresh = {})
        }
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        fun announced() = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.single()
        assertNull(announced())
        refreshing = true
        rule.waitForIdle()
        assertEquals("Generating summary…", announced())
        refreshing = false
        rule.waitForIdle()
        assertEquals("Summary updated.", announced())
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiSummaryCard(
                title = "Compte",
                summary = "",
                generatedAt = Now - 3 * Hour,
                now = Now,
                onRefresh = {},
                labels = MonarchAiSummaryCardLabels(
                    aiSummary = "Résumé IA",
                    refresh = "Actualiser",
                    generated = { "Rédigé $it" },
                    empty = "Pas encore de résumé.",
                ),
            )
        }
        rule.onNodeWithText("Résumé IA", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Actualiser").assertExists()
        rule.onNodeWithText("Rédigé 3 hours ago").assertExists()
        rule.onNodeWithText("Pas encore de résumé.").assertExists()
    }

    @Test
    fun refreshIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiSummaryCard(title = "Deal", summary = "Text.", onRefresh = {})
        }
        rule.onNodeWithText("Deal").assertIsBefore(rule.onNodeWithText("Refresh"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontSize() {
        val title = "Summary of the renewal negotiation with Acme"
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiSummaryCard(title = title, summary = "Short.") }
        rule.onNodeWithText(title).assertTextFullyVisible()
    }

    @Test
    fun theLogicDecidesBusyBodyAndAnnouncements() {
        val t = MonarchAiSummaryCardTokens
        val ready = MonarchAiSummaryCardStatus.Ready
        assertTrue(t.busy(MonarchAiSummaryCardStatus.Generating, refreshing = false))
        assertTrue(t.busy(ready, refreshing = true))
        assertFalse(t.busy(MonarchAiSummaryCardStatus.Stale, refreshing = false))
        assertEquals(MonarchAiSummaryCardBody.Summary, t.body("x", busy = true, status = ready))
        assertEquals(MonarchAiSummaryCardBody.Placeholder, t.body(" ", busy = true, status = ready))
        assertEquals(MonarchAiSummaryCardBody.Nothing, t.body("", busy = false, MonarchAiSummaryCardStatus.Error))
        assertEquals(MonarchAiSummaryCardBody.Empty, t.body("", busy = false, status = ready))
        assertNull(t.announcement(wasBusy = false, busy = false, status = ready, generating = "G", updated = "U"))
        assertEquals("G", t.announcement(wasBusy = false, busy = true, status = ready, generating = "G", updated = "U"))
        assertEquals("U", t.announcement(wasBusy = true, busy = false, status = ready, generating = "G", updated = "U"))
        assertEquals(
            "",
            t.announcement(true, busy = false, MonarchAiSummaryCardStatus.Error, generating = "G", updated = "U"),
        )
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, MonarchAiSummaryCardTokens.titleColor(c), theme.id)
            assertEquals(c.textMuted, MonarchAiSummaryCardTokens.mutedColor(c), theme.id)
        }
    }
}
