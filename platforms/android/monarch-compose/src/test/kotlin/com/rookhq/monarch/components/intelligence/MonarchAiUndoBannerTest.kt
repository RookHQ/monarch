package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aiundobanner.MonarchAiChange
import com.rookhq.monarch.components.intelligence.aiundobanner.MonarchAiUndoBanner
import com.rookhq.monarch.components.intelligence.aiundobanner.MonarchAiUndoBannerLabels
import com.rookhq.monarch.components.intelligence.aiundobanner.MonarchAiUndoBannerState
import com.rookhq.monarch.components.intelligence.aiundobanner.MonarchAiUndoBannerTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val TwoChanges = listOf(MonarchAiChange("Status", "Open", "Closed"), MonarchAiChange("Owner", "Ana", "Sam"))
private val FiveChanges = List(5) { MonarchAiChange("Field ${it + 1}", "Old", "New") }

internal object MonarchAiUndoBannerFixtures : ComponentFixtures {
    override val id = "ai-undo-banner"
    override val fixtures = listOf(
        fixture("title and description with icon") {
            MonarchAiUndoBanner(
                title = "AI changed the deal",
                onUndo = {},
                description = "On Acme Ltd",
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("two changes, audit and dismiss") {
            MonarchAiUndoBanner(
                title = "AI changed 2 fields",
                onUndo = {},
                changes = TwoChanges,
                onAudit = {},
                onDismiss = {},
            )
        },
        fixture("five changes in a disclosure") {
            MonarchAiUndoBanner(title = "AI changed 5 fields", onUndo = {}, changes = FiveChanges)
        },
        fixture("long values") {
            MonarchAiUndoBanner(
                title = "AI rewrote the summary",
                onUndo = {},
                changes = listOf(
                    MonarchAiChange("Summary", "Renewal pending legal review", "Renewal signed by both parties"),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiUndoBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsReadAsAnAiChangeWithItsDescription() {
        rule.setMonarchContent {
            MonarchAiUndoBanner(title = "AI changed 2 fields", onUndo = {}, description = "On Acme Ltd")
        }
        rule.onNodeWithContentDescription("AI change: AI changed 2 fields, On Acme Ltd").assertExists()
    }

    @Test
    fun eachChangeIsOneElementReadWithBeforeAndAfter() {
        rule.setMonarchContent { MonarchAiUndoBanner(title = "AI changed", onUndo = {}, changes = TwoChanges) }
        rule.onNodeWithContentDescription("Status changed from Open to Closed").assertExists()
        rule.onNodeWithContentDescription("Owner changed from Ana to Sam").assertExists()
    }

    @Test
    fun moreThanThreeChangesAreInADisclosure() {
        rule.setMonarchContent { MonarchAiUndoBanner(title = "AI changed", onUndo = {}, changes = FiveChanges) }
        rule.onNodeWithContentDescription("Field 1 changed from Old to New").assertDoesNotExist()
        rule.onNodeWithText("Show 5 changes").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Field 5 changed from Old to New").assertExists()
    }

    @Test
    fun undoIsBusyWhileItRunsAndIgnoresTaps() {
        var undos = 0
        rule.setMonarchContent { MonarchAiUndoBanner(title = "AI", onUndo = { undos++; awaitCancellation() }) }
        val undo = rule.onNodeWithText("Undo")
        undo.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("Loading", undo.stateDescription())
        undo.performClick()
        rule.waitForIdle()
        assertEquals(1, undos)
    }

    @Test
    fun aSuccessfulUndoIsAnnouncedAndUndoGoesAway() {
        rule.setMonarchContent { MonarchAiUndoBanner(title = "AI", onUndo = {}, onAudit = {}) }
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Undo").assertDoesNotExist()
        rule.onNodeWithText("View in audit log").assertExists()
        rule.onNodeWithContentDescription("Changes undone")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aFailedUndoIsAnnouncedAndCanRunAgain() {
        var undos = 0
        rule.setMonarchContent { MonarchAiUndoBanner(title = "AI", onUndo = { undos++; throw IOException("offline") }) }
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Couldn’t undo. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals(2, undos)
    }

    @Test
    fun theTimeLimitExpiresOnceAndRemovesUndo() {
        var expired = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiUndoBanner(title = "AI", onUndo = {}, durationMillis = 5_000L, onExpire = { expired++ })
        }
        rule.mainClock.advanceTimeBy(4_000L)
        assertEquals(0, expired)
        rule.onNodeWithText("Undo").assertExists()
        rule.mainClock.advanceTimeBy(1_500L)
        assertEquals(1, expired)
        rule.onNodeWithText("Undo").assertDoesNotExist()
        rule.onNodeWithContentDescription("Undo is no longer available.").assertExists()
        rule.mainClock.advanceTimeBy(10_000L)
        assertEquals(1, expired)
    }

    @Test
    fun theTimeLimitPausesWhileTheChangesAreOpen() {
        var expired = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiUndoBanner(
                title = "AI",
                onUndo = {},
                changes = FiveChanges,
                durationMillis = 5_000L,
                onExpire = { expired++ },
            )
        }
        rule.mainClock.advanceTimeBy(2_000L)
        rule.onNodeWithText("Show 5 changes").performClick()
        rule.mainClock.advanceTimeBy(20_000L)
        assertEquals(0, expired)
        rule.onNodeWithText("Show 5 changes").performClick()
        rule.mainClock.advanceTimeBy(3_500L)
        assertEquals(1, expired)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiUndoBanner(
                title = "IA",
                onUndo = {},
                changes = listOf(MonarchAiChange("Estado", "Abierto", "Cerrado")),
                onAudit = {},
                onDismiss = {},
                labels = MonarchAiUndoBannerLabels(
                    aiPrefix = "Cambio de IA:",
                    undo = "Deshacer",
                    audit = "Ver registro",
                    dismiss = "Cerrar",
                    change = { label, before, after -> "$label: de $before a $after" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Cambio de IA: IA").assertExists()
        rule.onNodeWithContentDescription("Estado: de Abierto a Cerrado").assertExists()
        rule.onNodeWithText("Deshacer").assertExists()
        rule.onNodeWithText("Ver registro").assertExists()
        rule.onNodeWithText("Cerrar").assertExists()
    }

    @Test
    fun dismissAndAuditCallBack() {
        var dismissed = 0
        var audited = 0
        rule.setMonarchContent {
            MonarchAiUndoBanner(title = "AI", onUndo = {}, onAudit = { audited++ }, onDismiss = { dismissed++ })
        }
        rule.onNodeWithText("Dismiss").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("View in audit log").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, dismissed)
        assertEquals(1, audited)
    }

    @Test
    fun undoComesBeforeTheAuditLinkRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiUndoBanner(title = "AI", onUndo = {}, onAudit = {})
        }
        rule.onNodeWithText("Undo").assertIsBefore(rule.onNodeWithText("View in audit log"), LayoutDirection.Rtl)
    }

    @Test
    fun longValuesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiUndoBanner(
                title = "AI rewrote the summary of the account",
                onUndo = {},
                changes = listOf(MonarchAiChange("Summary", "Renewal pending legal review", "Renewal signed")),
            )
        }
        rule.onNodeWithText("AI rewrote the summary of the account", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Renewal pending legal review", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicOfTheStates() {
        val t = MonarchAiUndoBannerTokens
        assertTrue(t.counting(1_000L, MonarchAiUndoBannerState.Active, changesOpen = false))
        assertTrue(t.counting(1_000L, MonarchAiUndoBannerState.Failed, changesOpen = false))
        assertFalse(t.counting(null, MonarchAiUndoBannerState.Active, changesOpen = false))
        assertFalse(t.counting(1_000L, MonarchAiUndoBannerState.Undoing, changesOpen = false))
        assertFalse(t.counting(1_000L, MonarchAiUndoBannerState.Active, changesOpen = true))
        assertFalse(t.showsUndo(MonarchAiUndoBannerState.Expired))
        assertTrue(t.showsUndo(MonarchAiUndoBannerState.Undoing))
        assertFalse(t.canUndo(MonarchAiUndoBannerState.Undoing))
        assertFalse(t.collapsesChanges(3))
        assertTrue(t.collapsesChanges(4))
        assertEquals("→", t.arrow(LayoutDirection.Ltr))
        assertEquals("←", t.arrow(LayoutDirection.Rtl))
    }

    @Test
    fun theColoursAreTheAiRolesThenNeutralInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val active = MonarchAiUndoBannerTokens.colors(MonarchAiUndoBannerState.Active, c)
            val ai = MonarchTonePalette.of(MonarchTone.Ai, c)
            assertEquals(ai.subtle, active.container, theme.id)
            assertEquals(ai.border, active.border, theme.id)
            assertEquals(c.aiStrong, active.icon, theme.id)
            val undone = MonarchAiUndoBannerTokens.colors(MonarchAiUndoBannerState.Undone, c)
            assertEquals(c.bgSubtle, undone.container, theme.id)
            assertEquals(c.borderDefault, undone.border, theme.id)
            assertEquals(c.textMuted, undone.status, theme.id)
            val failed = MonarchAiUndoBannerTokens.colors(MonarchAiUndoBannerState.Failed, c)
            assertEquals(c.dangerStrong, failed.status, theme.id)
        }
    }
}
