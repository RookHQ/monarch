package dev.monarchui.components.collaboration

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.postcard.MonarchPostCard
import dev.monarchui.components.collaboration.postcard.MonarchPostCardLabels
import dev.monarchui.components.collaboration.postcard.MonarchPostCardTag
import dev.monarchui.components.collaboration.postcard.MonarchPostCardTokens
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
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Now = 1_700_000_000_000L
private const val Hour = 3_600_000L

internal object MonarchPostCardFixtures : ComponentFixtures {
    override val id = "post-card"
    override val fixtures = listOf(
        fixture("title only") {
            MonarchPostCard(title = "Welcome to the community", author = "Ana Lima", onClick = {})
        },
        fixture("pinned, answered, tags, counts") {
            MonarchPostCard(
                title = "How do you plan a release?",
                author = "Ben Okafor",
                onClick = {},
                pinned = true,
                answered = true,
                publishedAt = Now - 3 * Hour,
                now = Now,
                tags = listOf(MonarchPostCardTag("Planning"), MonarchPostCardTag("In review", MonarchTone.Warning)),
                reactionCount = 1204,
                commentCount = 1,
            )
        },
        fixture("excerpt and an action") {
            MonarchPostCard(
                title = "Release notes",
                author = "Chloé",
                onClick = {},
                excerpt = "We moved the release to Thursday so both teams can test the new API together.",
                excerptLength = 60,
                commentCount = 0,
                actions = { MonarchButton(text = "Save", onClick = {}) },
            )
        },
        fixture("every tone") {
            MonarchPostCard(
                title = "Tones",
                author = "Dev",
                onClick = {},
                tags = MonarchTone.entries.map { MonarchPostCardTag(it.name, it) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPostCardTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(onClick: () -> Unit = {}, saves: () -> Unit = {}) {
        rule.setMonarchContent {
            MonarchPostCard(
                title = "How do you plan a release?",
                author = "Ana",
                onClick = onClick,
                pinned = true,
                answered = true,
                excerpt = "  We ship every two weeks.  ",
                publishedAt = Now - 3 * Hour,
                now = Now,
                tags = listOf(MonarchPostCardTag("Planning")),
                reactionCount = 12,
                commentCount = 4,
                actions = { MonarchButton(text = "Save", onClick = saves) },
            )
        }
    }

    @Test
    fun theAuthorLineTitleAndExcerptAreOneButtonReadInOrder() {
        var opened = 0
        show(onClick = { opened++ })
        val button = rule.onNode(hasClickAction() and hasText("How do you plan a release?"))
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        val text = button.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val expected = listOf(
            "Ana",
            "3 hours ago",
            "Pinned",
            "Answered",
            "Planning",
            "How do you plan a release?",
            "We ship every two weeks.",
        )
        assertEquals(expected, text)
        button.performClick()
        assertEquals(1, opened)
    }

    @Test
    fun theCountsAreOneElementAndTheActionsStaySeparate() {
        var saves = 0
        var opened = 0
        show(onClick = { opened++ }, saves = { saves++ })
        rule.onNode(hasText("12 reactions") and hasText("4 comments")).assertExists()
        rule.onNodeWithText("Save").performClick()
        assertEquals(1, saves)
        assertEquals(0, opened)
    }

    @Test
    fun countsUseTheLocaleGroupingAndHideWhenNull() {
        rule.setMonarchContent {
            MonarchPostCard(title = "T", author = "A", onClick = {}, reactionCount = 1204, commentCount = 1)
        }
        rule.onNode(hasText("1,204 reactions") and hasText("1 comment")).assertExists()
    }

    @Test
    fun withoutCountsOrActionsThereIsNoFooter() {
        rule.setMonarchContent { MonarchPostCard(title = "T", author = "A", onClick = {}) }
        rule.onAllNodesWithText("reactions", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Pinned").assertCountEquals(0)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPostCard(
                title = "T",
                author = "A",
                onClick = {},
                pinned = true,
                answered = true,
                reactionCount = 2,
                commentCount = 3,
                labels = MonarchPostCardLabels(
                    pinned = "Fijado",
                    answered = "Respondida",
                    reactions = { "$it reacciones" },
                    comments = { "$it comentarios" },
                ),
            )
        }
        rule.onNode(hasText("Fijado") and hasText("Respondida")).assertExists()
        rule.onNode(hasText("2 reacciones") and hasText("3 comentarios")).assertExists()
    }

    @Test
    fun theAuthorComesBeforeTheTimeInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchPostCard(title = "T", author = "Ana", onClick = {}, publishedAt = Now - Hour, now = Now)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Ana", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("1 hour ago", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun theTitleWrapsAtTheLargestFontScale() {
        val title = "How do you plan a release when two teams change the same API?"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPostCard(title = title, author = "Ana", onClick = {}, modifier = Modifier.testTag("card"))
        }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun excerptsAreCutAtAWordLikeTheWeb() {
        val t = MonarchPostCardTokens
        assertEquals("Short text", t.truncateExcerpt("  Short text  ", 220))
        assertEquals("The quick brown fox…", t.truncateExcerpt("The quick brown fox jumps over", 22))
        assertEquals("Supercalifragilistic…", t.truncateExcerpt("Supercalifragilisticexpialidocious", 20))
        assertEquals("", t.truncateExcerpt("   ", 220))
    }

    @Test
    fun theTextUsesItsRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val text = MonarchPostCardTokens.textColors(colors)
            assertEquals(colors.textDefault, text.title, theme.id)
            assertEquals(colors.textMuted, text.meta, theme.id)
        }
    }
}
