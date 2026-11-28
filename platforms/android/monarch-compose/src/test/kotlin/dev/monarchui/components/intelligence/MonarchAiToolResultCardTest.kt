package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultCard
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultCardLabels
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultCardTokens
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultCardUndo
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchAiToolResultCardFixtures : ComponentFixtures {
    override val id = "ai-tool-result-card"
    override val fixtures = listOf(
        fixture("record created with icon, open and undo") {
            MonarchAiToolResultCard(
                title = "Acme Ltd",
                kind = MonarchAiToolResultKind.RecordCreated,
                description = "Added to Accounts",
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
                onOpen = {},
                onUndo = {},
            )
        },
        fixture("email sent with facts") {
            MonarchAiToolResultCard(
                title = "Renewal reminder",
                kind = MonarchAiToolResultKind.EmailSent,
                meta = listOf(MonarchKeyValueItem("To", "3 people"), MonarchKeyValueItem("Sent", "10:42")),
            )
        },
        fixture("event booked and ticket filed") {
            MonarchAiToolResultCard(title = "Quarterly review", kind = MonarchAiToolResultKind.EventBooked)
            MonarchAiToolResultCard(title = "Login fails on Safari", kind = MonarchAiToolResultKind.TicketFiled)
        },
        fixture("generic, long title") {
            MonarchAiToolResultCard(
                title = "Updated the delivery address of every open order for Northwind Traders",
                onOpen = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiToolResultCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theKindTitleAndDescriptionAreOneElement() {
        rule.setMonarchContent {
            MonarchAiToolResultCard(
                title = "Acme Ltd",
                kind = MonarchAiToolResultKind.RecordCreated,
                description = "Added to Accounts",
            )
        }
        rule.onNodeWithText("Record created").assertExists()
        rule.onNodeWithText("Acme Ltd").assertExists()
        rule.onNodeWithText("Acme Ltd").assert(
            SemanticsMatcher("merged") { node ->
                val texts = node.config[SemanticsProperties.Text].map { it.text }
                texts == listOf("Record created", "Acme Ltd", "Added to Accounts")
            },
        )
    }

    @Test
    fun everyKindHasItsLabel() {
        val expected = mapOf(
            MonarchAiToolResultKind.EmailSent to "Email sent",
            MonarchAiToolResultKind.EventBooked to "Event booked",
            MonarchAiToolResultKind.TicketFiled to "Ticket filed",
            MonarchAiToolResultKind.Generic to "Done",
        )
        rule.setMonarchContent {
            expected.keys.forEach { MonarchAiToolResultCard(title = it.name, kind = it) }
        }
        expected.values.forEach { rule.onNodeWithText(it, substring = false, useUnmergedTree = true).assertExists() }
    }

    @Test
    fun openIsALinkNamedWithTheTitle() {
        var opened = 0
        rule.setMonarchContent { MonarchAiToolResultCard(title = "Acme Ltd", onOpen = { opened++ }) }
        rule.onNodeWithContentDescription("Open Acme Ltd")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun undoIsBusyWhileItRunsAndIgnoresTaps() {
        var undos = 0
        rule.setMonarchContent {
            MonarchAiToolResultCard(title = "Acme Ltd", onUndo = { undos++; awaitCancellation() })
        }
        val button = rule.onNodeWithText("Undo")
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("Loading", button.stateDescription())
        button.performClick()
        rule.waitForIdle()
        assertEquals(1, undos)
    }

    @Test
    fun aSuccessfulUndoHidesTheActionsAndIsAnnounced() {
        rule.setMonarchContent { MonarchAiToolResultCard(title = "Acme Ltd", onOpen = {}, onUndo = {}) }
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Undo").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open Acme Ltd").assertDoesNotExist()
        rule.onNodeWithContentDescription("Undone")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aFailedUndoIsAnnouncedAndUndoStays() {
        var undos = 0
        rule.setMonarchContent {
            MonarchAiToolResultCard(title = "Acme Ltd", onUndo = { undos++; throw IOException("offline") })
        }
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Couldn’t undo. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals(2, undos)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiToolResultCard(
                title = "Acme",
                kind = MonarchAiToolResultKind.RecordCreated,
                onOpen = {},
                onUndo = {},
                labels = MonarchAiToolResultCardLabels(
                    kind = { if (it == MonarchAiToolResultKind.RecordCreated) "Registro creado" else null },
                    open = "Abrir",
                    openLabel = { "Abrir $it" },
                    undo = "Deshacer",
                ),
            )
        }
        rule.onNodeWithText("Registro creado", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Abrir Acme").assertExists()
        rule.onNodeWithText("Abrir", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Deshacer").assertExists()
    }

    @Test
    fun openComesBeforeUndoRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiToolResultCard(title = "Acme", onOpen = {}, onUndo = {})
        }
        rule.onNodeWithContentDescription("Open Acme")
            .assertIsBefore(rule.onNodeWithText("Undo"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontSize() {
        val title = "Updated the delivery address of every open order for Northwind Traders"
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiToolResultCard(title = title) }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicOfTheUndoStates() {
        val t = MonarchAiToolResultCardTokens
        assertTrue(t.canUndo(MonarchAiToolResultCardUndo.Idle))
        assertTrue(t.canUndo(MonarchAiToolResultCardUndo.Failed))
        assertFalse(t.canUndo(MonarchAiToolResultCardUndo.Pending))
        assertFalse(t.canUndo(MonarchAiToolResultCardUndo.Done))
        assertFalse(t.showsActions(MonarchAiToolResultCardUndo.Done))
        assertTrue(t.showsActions(MonarchAiToolResultCardUndo.Failed))
    }

    @Test
    fun theColoursComeFromTheSuccessAndMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchAiToolResultCardTokens
            assertEquals(c.successSubtle, t.tileColors(undone = false, c).container, theme.id)
            assertEquals(c.successStrong, t.tileColors(undone = false, c).content, theme.id)
            assertEquals(c.bgMuted, t.tileColors(undone = true, c).container, theme.id)
            assertEquals(c.textMuted, t.tileColors(undone = true, c).content, theme.id)
            assertEquals(c.dangerStrong, t.statusColor(MonarchAiToolResultCardUndo.Failed, c), theme.id)
            assertEquals(c.textMuted, t.statusColor(MonarchAiToolResultCardUndo.Done, c), theme.id)
        }
    }
}
