package dev.monarchui.components.datadisplay

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionListTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val facts = listOf(
    MonarchDescriptionItem("Owner", "Ada Lovelace"),
    MonarchDescriptionItem("Customer ID", "cus_9s6XKzkNRiz8i3", copyable = true),
    MonarchDescriptionItem("Close date", null),
)

internal object MonarchDescriptionListFixtures : ComponentFixtures {
    override val id = "description-list"
    override val fixtures = listOf(
        fixture("stacked, copy and empty") { MonarchDescriptionList(items = facts) },
        fixture("inline") { MonarchDescriptionList(items = facts, layout = MonarchDescriptionListLayout.Inline) },
        fixture("copy icon") {
            MonarchDescriptionList(
                items = facts.take(2),
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("two columns and a wide item") {
            MonarchDescriptionList(
                items = facts.take(2) + MonarchDescriptionItem("Notes", "Renewal in March", wide = true),
                columns = 2,
            )
        },
        fixture("truncated value") {
            MonarchDescriptionList(
                items = listOf(
                    MonarchDescriptionItem(
                        "Notes",
                        "Asked for a second call with the finance team before signing the annual contract.",
                        maxLines = 1,
                    ),
                ),
            )
        },
        fixture("custom content") {
            MonarchDescriptionList(
                items = listOf(MonarchDescriptionItem("Stage", "Won", content = { MonarchBadge(text = "Won") })),
            )
        },
        fixture("long term and value in a narrow width") {
            MonarchDescriptionList(
                items = listOf(
                    MonarchDescriptionItem(
                        "Primary billing contact for invoices",
                        "accounts-payable-team@example-holdings.com",
                    ),
                ),
                modifier = Modifier.width(200.dp),
                layout = MonarchDescriptionListLayout.Inline,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDescriptionListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachItemIsReadAsItsTermThenItsValue() {
        rule.setMonarchContent { MonarchDescriptionList(items = facts) }
        rule.onNode(hasText("Owner") and hasText("Ada Lovelace")).assertExists()
    }

    @Test
    fun anEmptyValueIsADashReadAsNotSet() {
        rule.setMonarchContent { MonarchDescriptionList(items = facts) }
        // Regression (C-043): the word was a content description on a cleared
        // node, which TalkBack reads instead of the merged label. It is now
        // the node's text, so the row reads "Close date, Not set".
        rule.onNode(hasText("Close date") and hasText("Not set")).assertExists()
        rule.onNode(hasText("Close date") and hasContentDescription("Not set")).assertDoesNotExist()
        rule.onNodeWithText("—").assertDoesNotExist()
    }

    @Test
    fun aCopyableValueHasItsOwnCopyButton() {
        rule.setMonarchContent { MonarchDescriptionList(items = facts) }
        rule.onNodeWithContentDescription("Copy Customer ID")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
        assertEquals("cus_9s6XKzkNRiz8i3", clip?.getItemAt(0)?.text?.toString())
        rule.onAllNodesWithContentDescription("Copy", substring = true).assertCountEquals(1)
    }

    @Test
    fun theBuiltInEmptyLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchDescriptionList(items = facts, emptyLabel = "Non défini") }
        rule.onNode(hasText("Close date") and hasText("Non défini")).assertExists()
    }

    @Test
    fun aLongValueIsTruncatedWithShowMore() {
        rule.setMonarchContent {
            MonarchDescriptionList(
                items = listOf(
                    MonarchDescriptionItem(
                        "Notes",
                        "Asked for a second call with the finance team before signing the annual contract.",
                        maxLines = 1,
                    ),
                ),
                showMoreLabel = "More",
            )
        }
        rule.onNodeWithText("More").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun inlinePutsTheTermBeforeTheValueInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchDescriptionList(items = facts.take(1), layout = MonarchDescriptionListLayout.Inline)
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Owner", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Ada Lovelace", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun inlineStacksAtTheLargestFontSizeAndNothingIsCut() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDescriptionList(items = facts.take(1), layout = MonarchDescriptionListLayout.Inline)
        }
        val term = rule.onNodeWithText("Owner", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = rule.onNodeWithText("Ada Lovelace", useUnmergedTree = true)
        value.assertTextFullyVisible()
        assertTrue(value.getUnclippedBoundsInRoot().top >= term.bottom, "The value is under the term")
    }

    @Test
    fun itemsFillRowsOfTheColumnCountAndAWideItemTakesARow() {
        val items = listOf(
            MonarchDescriptionItem("A", "1"),
            MonarchDescriptionItem("B", "2"),
            MonarchDescriptionItem("C", "3", wide = true),
            MonarchDescriptionItem("D", "4"),
        )
        assertEquals(listOf(listOf(0, 1), listOf(2), listOf(3)), MonarchDescriptionListTokens.rows(items, 2))
        assertEquals(listOf(listOf(0), listOf(1), listOf(2), listOf(3)), MonarchDescriptionListTokens.rows(items, 1))
        assertEquals(1, MonarchDescriptionListTokens.columns(3, widthDp = 250f))
        assertEquals(3, MonarchDescriptionListTokens.columns(3, widthDp = 800f))
        assertEquals(
            MonarchDescriptionListLayout.Stacked,
            MonarchDescriptionListTokens.layout(MonarchDescriptionListLayout.Inline, 400f, fontScale = 2f),
        )
        assertEquals(
            MonarchDescriptionListLayout.Inline,
            MonarchDescriptionListTokens.layout(MonarchDescriptionListLayout.Inline, 400f, fontScale = 1f),
        )
    }

    @Test
    fun columnsAreSideBySideWhenWide() {
        rule.setMonarchContent {
            MonarchDescriptionList(
                items = listOf(MonarchDescriptionItem("A", "1"), MonarchDescriptionItem("B", "2")),
                modifier = Modifier.requiredWidth(700.dp),
                columns = 2,
            )
        }
        val a = rule.onNodeWithText("A", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val b = rule.onNodeWithText("B", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(a.top, b.top)
    }

    @Test
    fun theCopyTextFollowsTheItem() {
        assertEquals("x", MonarchDescriptionListTokens.copyText(MonarchDescriptionItem("T", "x", copyable = true)))
        val shortened = MonarchDescriptionItem("T", "short", copyValue = "full")
        assertEquals("full", MonarchDescriptionListTokens.copyText(shortened))
        assertNull(MonarchDescriptionListTokens.copyText(MonarchDescriptionItem("T", "x")))
        assertNull(MonarchDescriptionListTokens.copyText(MonarchDescriptionItem("T", "", copyable = true)))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchDescriptionListTokens.termColor(colors), theme.id)
            assertEquals(colors.textDefault, MonarchDescriptionListTokens.valueColor(false, colors), theme.id)
            assertEquals(colors.textSubtle, MonarchDescriptionListTokens.valueColor(true, colors), theme.id)
        }
    }
}
