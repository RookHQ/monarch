package com.rookhq.monarch.components.collaboration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.typingindicator.MonarchTypingIndicator
import com.rookhq.monarch.components.collaboration.typingindicator.MonarchTypingIndicatorTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchTypingIndicatorFixtures : ComponentFixtures {
    override val id = "typing-indicator"
    override val fixtures = listOf(
        fixture("one person") { MonarchTypingIndicator(names = listOf("Ana")) },
        fixture("two people") { MonarchTypingIndicator(names = listOf("Ana", "Ben")) },
        fixture("several people") { MonarchTypingIndicator(names = listOf("Ana", "Ben", "Chen")) },
        fixture("nobody, space reserved") { MonarchTypingIndicator(names = emptyList()) },
        fixture("long names") {
            MonarchTypingIndicator(names = listOf("Maria Fernanda de los Santos", "Alexandria Konstantinopoulou"))
        },
        fixture("custom text") { MonarchTypingIndicator(names = listOf("Ana"), text = "Ana écrit…") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTypingIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private var names by mutableStateOf(listOf<String>())

    private fun show(announceChanges: Boolean = true, reserveSpace: Boolean = true, text: String? = null) {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchTypingIndicator(
                names = names,
                modifier = Modifier.testTag("typing"),
                text = text,
                reserveSpace = reserveSpace,
                announceChanges = announceChanges,
            )
        }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun setNames(vararg next: String) {
        names = next.toList()
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(FRAMES)
    }

    private companion object {
        const val FRAMES = 64L
    }

    @Test
    fun theSentenceIsWordedForOneTwoOrSeveralPeople() {
        show()
        setNames("Ana")
        rule.onNodeWithTag("typing").assertTextEquals("Ana is typing…")
        setNames("Ana", "Ben")
        rule.onNodeWithTag("typing").assertTextEquals("Ana and Ben are typing…")
        setNames("Ana", "Ben", "Chen")
        rule.onNodeWithTag("typing").assertTextEquals("Several people are typing…")
    }

    @Test
    fun theSentenceCanBeReplaced() {
        names = listOf("Ana")
        show(text = "Ana écrit…")
        rule.onNodeWithTag("typing").assertTextEquals("Ana écrit…")
    }

    @Test
    fun theSameElementIsAPoliteLiveRegionWhileNobodyAndSomebodyIsTyping() {
        show()
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNodeWithTag("typing").assert(live)
        rule.onNodeWithTag("typing").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        setNames("Ana")
        rule.onNodeWithTag("typing").assert(live).assertTextEquals("Ana is typing…")
    }

    @Test
    fun announcementsCanBeTurnedOff() {
        names = listOf("Ana")
        show(announceChanges = false)
        rule.onNodeWithTag("typing").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun theHeightOfOneLineIsKeptWhileNobodyIsTypingUnlessAskedNotTo() {
        show(reserveSpace = true)
        val reserved = rule.onNodeWithTag("typing").fetchSemanticsNode().size.height
        setNames("Ana")
        val typing = rule.onNodeWithTag("typing").fetchSemanticsNode().size.height
        assertTrue(reserved > 0, "space is reserved")
        assertEquals(typing, reserved, "the line does not change height when someone starts typing")
    }

    @Test
    fun nothingIsReservedWhenNotAsked() {
        show(reserveSpace = false)
        assertEquals(0, rule.onNodeWithTag("typing").fetchSemanticsNode().size.height)
    }

    @Test
    fun theSentenceWrapsAtTheLargestFontScale() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTypingIndicator(names = listOf("Maria Fernanda", "Alexandria Konstantinopoulou"))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Maria Fernanda and Alexandria Konstantinopoulou are typing…", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theDotsComeBeforeTheSentenceInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            MonarchTypingIndicator(names = listOf("Ana"), modifier = Modifier.testTag("typing"))
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.mainClock.advanceTimeByFrame()
            val whole = rule.onNodeWithTag("typing").getUnclippedBoundsInRoot()
            val text = rule.onNodeWithText("Ana is typing…", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val room = MonarchTypingIndicatorTokens.DotSize * MonarchTypingIndicatorTokens.DOT_COUNT
            val follows = if (direction == LayoutDirection.Ltr) {
                text.left >= whole.left + room
            } else {
                text.right <= whole.right - room
            }
            assertTrue(follows, "$direction: the sentence follows the dots")
        }
    }

    @Test
    fun theWordingRuleIsTheSameAsOnTheWeb() {
        fun sentence(vararg names: String) = MonarchTypingIndicatorTokens.sentence(
            names = names.toList(),
            one = { "1:$it" },
            two = { a, b -> "2:$a,$b" },
            several = { "n" },
        )
        assertEquals("", sentence())
        assertEquals("1:Ana", sentence("Ana"))
        assertEquals("2:Ana,Ben", sentence("Ana", "Ben"))
        assertEquals("n", sentence("Ana", "Ben", "Chen", "Dee"))
    }

    @Test
    fun theTextAndDotsUseMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchTypingIndicatorTokens.textColor(colors), theme.id)
            assertEquals(colors.textSubtle, MonarchTypingIndicatorTokens.restingDotColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchTypingIndicatorTokens.activeDotColor(colors), theme.id)
        }
    }
}
