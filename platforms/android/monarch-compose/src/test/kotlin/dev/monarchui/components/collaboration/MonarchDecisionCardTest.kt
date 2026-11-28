package dev.monarchui.components.collaboration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.decisioncard.MonarchDecision
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionCard
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionCardLabels
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionCardTokens
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionOption
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val keep = MonarchDecisionOption(label = "Keep", value = "keep", tone = MonarchTone.Success)
private val lapse = MonarchDecisionOption(
    label = "Let lapse",
    value = "lapse",
    note = "Email stops working.",
    tone = MonarchTone.Danger,
    reasonRequired = true,
)
private val review = MonarchDecisionOption(label = "Review", value = "review", enabled = false)
private val options = listOf(keep, lapse)

// The test window is 320 by 470dp and fixtures do not scroll, so each fixture fits at font scale 2.0.
internal object MonarchDecisionCardFixtures : ComponentFixtures {
    override val id = "decision-card"
    override val fixtures = listOf(
        fixture("form, nothing decided") {
            MonarchDecisionCard(options = options, value = null, allowNone = false, onDecide = {})
        },
        fixture("form, a disabled choice") {
            MonarchDecisionCard(options = listOf(keep, review), value = "keep", allowNone = false, onDecide = {})
        },
        fixture("form, saving") {
            MonarchDecisionCard(options = listOf(keep), value = null, allowNone = false, saving = true, onDecide = {})
        },
        fixture("form, failed") {
            MonarchDecisionCard(
                options = listOf(keep),
                value = null,
                allowNone = false,
                errorText = "Not saved.",
                onDecide = {},
            )
        },
        fixture("read-only with reason and author") {
            MonarchDecisionCard(
                options = options,
                value = "lapse",
                title = "Renewal",
                reason = "Replaced by the new brand.",
                decidedBy = "Ana",
                readOnly = true,
            )
        },
        fixture("read-only, undecided") { MonarchDecisionCard(options = options, value = null) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDecisionCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val decisions = mutableListOf<MonarchDecision>()

    private fun showForm(
        value: String? = null,
        reason: String? = null,
        reasonRequired: Boolean = false,
        reasonLimit: Int = MonarchDecisionCardTokens.DEFAULT_REASON_LIMIT,
        allowNone: Boolean = true,
    ) {
        rule.setMonarchContent {
            MonarchDecisionCard(
                options = options + review,
                value = value,
                title = "Renewal decision",
                reason = reason,
                allowNone = allowNone,
                reasonRequired = reasonRequired,
                reasonLimit = reasonLimit,
                onDecide = { decisions += it },
            )
        }
    }

    private fun click(text: String) = rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)

    private fun reasonField() = rule.onNode(hasSetTextAction())

    @Test
    fun theTitleIsAHeadingAndTheChoicesAreRadioButtons() {
        showForm(value = "keep")
        rule.onNodeWithText("Renewal decision").assert(isHeading())
        rule.onNodeWithText("Keep")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Review").assertIsNotEnabled()
        rule.onNodeWithText("No decision").assertExists()
    }

    @Test
    fun saveIsDisabledUntilSomethingChangesThenPassesTheDecision() {
        showForm(value = "keep")
        rule.onNodeWithText("Save decision").assertIsNotEnabled()
        click("No decision")
        rule.onNodeWithText("Save decision").assertIsEnabled()
        click("Save decision")
        assertEquals(listOf(MonarchDecision(value = null, reason = null)), decisions)
    }

    @Test
    fun aRequiredReasonIsAskedForBeforeSaving() {
        showForm()
        click("Let lapse")
        click("Save decision")
        assertTrue(decisions.isEmpty())
        rule.onNodeWithText("Add a reason for this decision.", useUnmergedTree = true).assertExists()
        reasonField().performTextInput("  Replaced by the new brand.  ")
        click("Save decision")
        assertEquals(listOf(MonarchDecision(value = "lapse", reason = "Replaced by the new brand.")), decisions)
    }

    @Test
    fun theNoteOfTheSelectedChoiceIsAPoliteLiveRegion() {
        showForm()
        rule.onAllNodesWithText("Email stops working.").assertCountEquals(0)
        click("Let lapse")
        rule.onNodeWithText("Email stops working.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theReasonIsCutAtTheLimitAndTheCharactersLeftAreShownNearIt() {
        showForm(value = "keep", reasonLimit = 60)
        reasonField().performTextReplacement("x".repeat(15))
        rule.onNodeWithText("45 characters left", useUnmergedTree = true).assertExists()
        reasonField().performTextReplacement("y".repeat(80))
        rule.onNodeWithText("0 characters left", useUnmergedTree = true).assertExists()
        click("Save decision")
        assertEquals("y".repeat(60), decisions.single().reason)
    }

    @Test
    fun theNoDecisionChoiceCanBeLeftOut() {
        showForm(allowNone = false)
        rule.onAllNodesWithText("No decision").assertCountEquals(0)
        rule.onNodeWithText("Save decision").assertIsNotEnabled()
    }

    @Test
    fun whileSavingTheFormIsDisabledAndTheEndOfASaveIsAnnounced() {
        var saving by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchDecisionCard(options = options, value = "keep", saving = saving, onDecide = {})
        }
        rule.onNodeWithText("Keep").assertIsNotEnabled()
        assertEquals("Loading", rule.onNodeWithText("Save decision").stateDescription())
        rule.onAllNodesWithText("Decision saved.").assertCountEquals(0)
        saving = false
        rule.waitForIdle()
        rule.onNodeWithText("Decision saved.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        click("Let lapse")
        rule.onAllNodesWithText("Decision saved.").assertCountEquals(0)
    }

    @Test
    fun aFailureIsShownAndTheChoiceIsKept() {
        rule.setMonarchContent {
            MonarchDecisionCard(options = options, value = "keep", errorText = "Not saved. Try again.", onDecide = {})
        }
        rule.onNodeWithText("Not saved. Try again.").assertExists()
        rule.onAllNodesWithText("Decision saved.").assertCountEquals(0)
    }

    @Test
    fun aNewSavedDecisionFromTheAppResetsTheForm() {
        var value by mutableStateOf<String?>("keep")
        rule.setMonarchContent {
            MonarchDecisionCard(options = options, value = value, onDecide = {})
        }
        click("No decision")
        rule.onNodeWithText("No decision").assertIsSelected()
        value = "lapse"
        rule.waitForIdle()
        rule.onNodeWithText("Let lapse").assertIsSelected()
        rule.onNodeWithText("Save decision").assertIsNotEnabled()
    }

    @Test
    fun theReadOnlyCardShowsABadgeTheReasonAndWhoDecided() {
        rule.setMonarchContent {
            MonarchDecisionCard(
                options = options,
                value = "lapse",
                reason = "Replaced by the new brand.",
                decidedAt = System.currentTimeMillis() - 3 * 60 * 60 * 1000L,
                decidedBy = "Ana",
            )
        }
        rule.onNodeWithText("Let lapse").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        rule.onNodeWithText("Replaced by the new brand.").assertExists()
        // Shown as "Decided 3 hours ago by Ana"; TalkBack reads the absolute date and time instead.
        rule.onNode(hasContentDescription("Decided ", substring = true))
            .assert(hasContentDescription("by Ana", substring = true))
            .assert(hasContentDescription("ago", substring = true).not())
        rule.onAllNodesWithText("Save decision").assertCountEquals(0)
    }

    @Test
    fun aReadOnlyCardWithoutADecisionSaysSo() {
        rule.setMonarchContent { MonarchDecisionCard(options = options, value = null, readOnly = true, onDecide = {}) }
        rule.onNodeWithText("No decision yet").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDecisionCard(
                options = options,
                value = "keep",
                decidedBy = "Ana",
                labels = MonarchDecisionCardLabels(
                    none = "Keine Entscheidung",
                    save = "Speichern",
                    decidedBy = { "von $it" },
                ),
                onDecide = {},
            )
        }
        rule.onNodeWithText("Keine Entscheidung").assertExists()
        rule.onNodeWithText("Speichern").assertExists()
        rule.onNode(hasContentDescription("von Ana")).assertExists()
    }

    @Test
    fun theBadgeComesBeforeTheReasonInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchDecisionCard(options = options, value = "keep", reason = "Fine", readOnly = true)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Keep", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Fine", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDecisionCard(
                options = options,
                value = "lapse",
                title = "Renewal decision for example.com",
                reason = "Replaced by the new brand.",
                readOnly = true,
            )
        }
        rule.onNodeWithText("Renewal decision for example.com", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Replaced by the new brand.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheFormAreTheSameAsOnTheWeb() {
        val t = MonarchDecisionCardTokens
        assertFalse(t.needsReason(null, options, reasonRequired = true))
        assertTrue(t.needsReason("keep", options, reasonRequired = true))
        assertFalse(t.needsReason("keep", options, reasonRequired = false))
        assertTrue(t.needsReason("lapse", options, reasonRequired = false))
        val optional = MonarchDecisionOption(label = "A", value = "a", reasonRequired = false)
        assertFalse(t.needsReason("a", listOf(optional), reasonRequired = true))
        assertFalse(t.dirty("keep", " Fine ", value = "keep", reason = "Fine"))
        assertTrue(t.dirty("lapse", "", value = "keep", reason = null))
        assertTrue(t.dirty("keep", "Other", value = "keep", reason = "Fine"))
        assertEquals(MonarchDecision("keep", null), t.decision("keep", "   "))
        assertEquals(MonarchDecision("keep", "Why"), t.decision("keep", " Why "))
        val now = 1_700_000_000_000L
        assertEquals("3 hours ago", t.relativeTime(now - 3 * 60 * 60 * 1000L, now))
        assertTrue(t.showsCharactersLeft(limit = 500, length = 450))
        assertFalse(t.showsCharactersLeft(limit = 500, length = 449))
    }

    @Test
    fun secondaryTextUsesTheMutedRoleInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchDecisionCardTokens.mutedText(colors), theme.id)
        }
    }
}
