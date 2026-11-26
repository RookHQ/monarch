package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceListTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
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

private val Items = listOf(
    MonarchConsequence(MonarchConsequenceKind.Kept, "Invoices stay in billing"),
    MonarchConsequence(MonarchConsequenceKind.Removed, "tasks will be deleted", count = 1284),
    MonarchConsequence(MonarchConsequenceKind.Warning, "This cannot be undone", detail = "Export first if needed."),
    MonarchConsequence(MonarchConsequenceKind.Changed, "Members lose access immediately"),
)

internal object MonarchConsequenceListFixtures : ComponentFixtures {
    override val id = "consequence-list"
    override val fixtures = listOf(
        fixture("ungrouped with icons and a heading") {
            MonarchConsequenceList(
                items = Items,
                heading = "What happens",
                icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("grouped") { MonarchConsequenceList(items = Items, grouped = true) },
        fixture("loading") { MonarchConsequenceList(items = Items, loading = true, heading = "What happens") },
        fixture("long text") {
            MonarchConsequenceList(
                items = listOf(
                    MonarchConsequence(
                        MonarchConsequenceKind.Removed,
                        "files in the shared drive of the team will be deleted for everyone",
                        detail = "Including the files that other teams link to from their own projects.",
                        count = 12000,
                    ),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchConsequenceListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun anUngroupedRowIsOneElementReadWithItsKind() {
        rule.setMonarchContent { MonarchConsequenceList(items = Items) }
        rule.onNodeWithText("1,284 tasks will be deleted")
            .assertTextEquals("Removed", "1,284 tasks will be deleted")
            .assertHasNoClickAction()
        rule.onNodeWithText("This cannot be undone")
            .assertTextEquals("Warning", "This cannot be undone", "Export first if needed.")
    }

    @Test
    fun theKindLabelReplacesTheBuiltInWords() {
        rule.setMonarchContent { MonarchConsequenceList(items = Items.take(1), kindLabel = { "Stays" }) }
        rule.onNodeWithText("Invoices stay in billing").assertTextEquals("Stays", "Invoices stay in billing")
    }

    @Test
    fun groupsFollowTheFixedOrderUnderHeadingsWithoutTheKindWord() {
        rule.setMonarchContent { MonarchConsequenceList(items = Items, grouped = true, heading = "What happens") }
        rule.onNodeWithText("What happens").assert(isHeading())
        val headings = listOf("Will be deleted", "Will change", "Before you continue", "Will be kept")
        headings.forEach { rule.onNodeWithText(it).assert(isHeading()) }
        val tops = headings.map { rule.onNodeWithText(it).getUnclippedBoundsInRoot().top }
        assertEquals(tops.sorted(), tops)
        rule.onNodeWithText("Invoices stay in billing").assertTextEquals("Invoices stay in billing")
    }

    @Test
    fun theGroupTitleReplacesTheBuiltInHeadings() {
        rule.setMonarchContent {
            MonarchConsequenceList(items = Items.take(1), grouped = true, groupTitle = { "Kept as is" })
        }
        rule.onNodeWithText("Kept as is").assert(isHeading())
    }

    @Test
    fun whileLoadingThePlaceholdersAreOneElement() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchConsequenceList(items = Items, loading = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Checking what will change").assertExists()
        rule.onNodeWithText("Invoices stay in billing").assertDoesNotExist()
    }

    @Test
    fun theLoadingLabelReplacesTheBuiltInString() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchConsequenceList(items = Items, loading = true, loadingLabel = "Counting tasks", heading = "Impact")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Counting tasks").assertContentDescriptionEquals("Counting tasks")
    }

    @Test
    fun theIconIsHiddenAndAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchConsequenceList(
                items = Items.take(1),
                icon = { Box(Modifier.testTag("icon").size(16.dp)) },
            )
        }
        rule.onNodeWithTag("icon").assertDoesNotExist()
        val icon = rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val text = rule.onNodeWithText("Invoices stay in billing", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(icon.left >= text.right)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchConsequenceList(
                items = listOf(
                    MonarchConsequence(
                        MonarchConsequenceKind.Removed,
                        "files in the shared drive will be deleted",
                        detail = "Including linked files.",
                        count = 12,
                    ),
                ),
            )
        }
        rule.onNodeWithText("12 files", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Including linked files.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursFollowTheKindInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val icon = { kind: MonarchConsequenceKind -> MonarchConsequenceListTokens.iconColor(kind, colors) }
            assertEquals(colors.dangerStrong, icon(MonarchConsequenceKind.Removed), theme.id)
            assertEquals(colors.textMuted, icon(MonarchConsequenceKind.Changed), theme.id)
            assertEquals(colors.warningStrong, icon(MonarchConsequenceKind.Warning), theme.id)
            assertEquals(colors.successStrong, icon(MonarchConsequenceKind.Kept), theme.id)
            assertEquals(
                colors.dangerStrong,
                MonarchConsequenceListTokens.countColor(MonarchConsequenceKind.Removed, colors),
                theme.id,
            )
            assertEquals(
                colors.textDefault,
                MonarchConsequenceListTokens.countColor(MonarchConsequenceKind.Kept, colors),
                theme.id,
            )
        }
    }
}
