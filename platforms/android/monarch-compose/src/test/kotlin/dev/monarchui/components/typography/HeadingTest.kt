package dev.monarchui.components.typography

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.textLayout
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The web Heading is MonarchText in a heading role, announced as a heading by TalkBack. */
internal object HeadingFixtures : ComponentFixtures {
    override val id = "heading"
    override val fixtures = listOf(
        fixture("every heading role") {
            Column {
                MonarchText(text = "Display", style = MonarchTextStyle.Display)
                MonarchText(text = "Billing", style = MonarchTextStyle.Title)
                MonarchText(text = "Payment methods", style = MonarchTextStyle.Heading)
                MonarchText(text = "Cards", style = MonarchTextStyle.Subheading)
            }
        },
        fixture("small headings and muted") {
            Column {
                MonarchText(text = "Card title", style = MonarchTextStyle.BodyStrong, heading = true)
                MonarchText(text = "Group label", style = MonarchTextStyle.Label, heading = true)
                MonarchText(
                    text = "Nothing here yet",
                    style = MonarchTextStyle.Subheading,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        },
        fixture("one line with an ellipsis") {
            MonarchText(
                text = "Quarterly revenue by region and product line, before tax",
                style = MonarchTextStyle.Heading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class HeadingTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val isNotHeading = SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading)

    @Test
    fun theHeadingRolesAreHeadingsForTalkBack() {
        rule.setMonarchContent {
            Column {
                MonarchText(text = "Display", style = MonarchTextStyle.Display)
                MonarchText(text = "Title", style = MonarchTextStyle.Title)
                MonarchText(text = "Heading", style = MonarchTextStyle.Heading)
                MonarchText(text = "Subheading", style = MonarchTextStyle.Subheading)
                MonarchText(text = "Body", style = MonarchTextStyle.Body)
            }
        }
        listOf("Display", "Title", "Heading", "Subheading").forEach { rule.onNodeWithText(it).assert(isHeading) }
        rule.onNodeWithText("Body").assert(isNotHeading)
    }

    @Test
    fun aSmallRoleBecomesAHeadingWithTheFlag() {
        rule.setMonarchContent {
            Column {
                MonarchText(text = "Card title", style = MonarchTextStyle.BodyStrong, heading = true)
                MonarchText(text = "Not a heading", style = MonarchTextStyle.Title, heading = false)
            }
        }
        rule.onNodeWithText("Card title").assert(isHeading)
        rule.onNodeWithText("Not a heading").assert(isNotHeading)
    }

    @Test
    fun aHeadingWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchText(text = "Payment methods and billing details", style = MonarchTextStyle.Title)
        }
        rule.onNodeWithText("Payment methods and billing details").assertTextFullyVisible()
        assertTrue(rule.onNodeWithText("Payment methods and billing details").textLayout().lineCount > 1)
    }

    @Test
    fun aOneLineHeadingEndsWithAnEllipsisAndKeepsItsWholeTextForTalkBack() {
        val text = "Quarterly revenue by region and product line, before tax"
        rule.setMonarchContent {
            MonarchText(text = text, style = MonarchTextStyle.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val layout = rule.onNodeWithText(text).textLayout()
        assertEquals(1, layout.lineCount)
        assertTrue(layout.isLineEllipsized(0))
    }
}
