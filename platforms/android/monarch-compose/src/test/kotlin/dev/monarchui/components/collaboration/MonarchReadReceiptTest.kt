package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceipt
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptDisplay
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptLabels
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptReader
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val Readers = listOf(
    MonarchReadReceiptReader(name = "Ana", id = "u1"),
    MonarchReadReceiptReader(name = "Ben", id = "u2"),
    MonarchReadReceiptReader(name = "Chloé", id = "u3"),
    MonarchReadReceiptReader(name = "Dev", id = "u4"),
    MonarchReadReceiptReader(name = "Eun-ji", id = "u5"),
)

internal object MonarchReadReceiptFixtures : ComponentFixtures {
    override val id = "read-receipt"
    override val fixtures = listOf(
        fixture("text") { MonarchReadReceipt(readers = Readers.take(1)) },
        fixture("count") { MonarchReadReceipt(readers = Readers, display = MonarchReadReceiptDisplay.Count) },
        fixture("avatars with more") {
            MonarchReadReceipt(readers = Readers, display = MonarchReadReceiptDisplay.Avatars)
        },
        fixture("button with a mark") {
            MonarchReadReceipt(readers = Readers.take(2), onShowReaders = {}, icon = { Box(Modifier) })
        },
        fixture("long custom text") {
            MonarchReadReceipt(
                readers = Readers.take(2),
                labels = MonarchReadReceiptLabels(seen = "Read by everyone in this conversation a moment ago"),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchReadReceiptTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyDisplayReadsTheFullListOfNames() {
        rule.setMonarchContent {
            Column {
                MonarchReadReceiptDisplay.entries.forEach { display ->
                    MonarchReadReceipt(
                        readers = Readers.take(3),
                        display = display,
                        modifier = Modifier.testTag(display.name),
                    )
                }
            }
        }
        MonarchReadReceiptDisplay.entries.forEach { display ->
            val expected = listOf("Seen by Ana, Ben and Chloé")
            rule.onNodeWithTag(display.name)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, expected))
        }
        rule.onAllNodesWithText("Seen", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithText("Seen by 3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun anEmptyListDrawsNothing() {
        rule.setMonarchContent { Box(Modifier.testTag("host")) { MonarchReadReceipt(readers = emptyList()) } }
        assertEquals(0, rule.onNodeWithTag("host").fetchSemanticsNode().children.size)
        rule.onAllNodesWithText("Seen", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun withoutACallbackTheReceiptIsNotAButton() {
        rule.setMonarchContent { MonarchReadReceipt(readers = Readers.take(1)) }
        val node = rule.onNodeWithContentDescription("Seen by Ana").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
        assertFalse(node.config.contains(SemanticsProperties.Role))
    }

    @Test
    fun withACallbackTheReceiptIsAButtonWithA48dpTarget() {
        var opened = 0
        rule.setMonarchContent { MonarchReadReceipt(readers = Readers.take(2), onShowReaders = { opened++ }) }
        rule.onNodeWithContentDescription("Seen by Ana and Ben")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun theAvatarsShowAtMostTheLimitThenTheRest() {
        rule.setMonarchContent {
            MonarchReadReceipt(readers = Readers, display = MonarchReadReceiptDisplay.Avatars, maxAvatars = 2)
        }
        rule.onNodeWithText("+3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun oneReaderOverTheLimitShowsEveryFace() {
        rule.setMonarchContent {
            MonarchReadReceipt(readers = Readers.take(4), display = MonarchReadReceiptDisplay.Avatars, maxAvatars = 3)
        }
        rule.onAllNodesWithText("+1", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("D", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchReadReceipt(
                readers = Readers.take(3),
                display = MonarchReadReceiptDisplay.Count,
                labels = MonarchReadReceiptLabels(
                    seenByCount = { "Visto por $it" },
                    seenBy = { "Visto por $it" },
                    list = { it.joinToString(" y ") },
                ),
            )
        }
        rule.onNodeWithText("Visto por 3", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Visto por Ana y Ben y Chloé").assertExists()
    }

    @Test
    fun theMarkComesBeforeTheTextInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchReadReceipt(readers = Readers.take(1), icon = { Box(Modifier.testTag("mark")) })
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithTag("mark", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Seen", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchReadReceipt(
                readers = Readers.take(1),
                labels = MonarchReadReceiptLabels(seen = "Read by everyone in this conversation a moment ago"),
            )
        }
        rule.onNodeWithText("Read by everyone in this conversation a moment ago", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun namesAreJoinedLikeTheWeb() {
        val join = { names: List<String> -> MonarchReadReceiptTokens.joinNames(names, ", ") { a, b -> "$a and $b" } }
        assertEquals("", join(emptyList()))
        assertEquals("Ana", join(listOf("Ana")))
        assertEquals("Ana and Ben", join(listOf("Ana", "Ben")))
        assertEquals("Ana, Ben and Chloé", join(listOf("Ana", "Ben", "Chloé")))
        assertEquals(1, MonarchReadReceiptTokens.avatarLimit(0))
        assertEquals(1, MonarchReadReceiptTokens.avatarLimit(-4))
        assertEquals(3, MonarchReadReceiptTokens.avatarLimit(3))
    }

    @Test
    fun theTextUsesTheMutedRoleInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchReadReceiptTokens.contentColor(colors), theme.id)
        }
    }
}
