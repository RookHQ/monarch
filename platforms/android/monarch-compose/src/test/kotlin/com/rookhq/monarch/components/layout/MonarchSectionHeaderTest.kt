package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeader
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeaderSize
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeaderTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchSectionHeaderFixtures : ComponentFixtures {
    override val id = "section-header"
    override val fixtures = MonarchSectionHeaderSize.entries.map { size ->
        fixture("title only, ${size.name}") { MonarchSectionHeader(title = "Recent invoices", size = size) }
    } + listOf(
        fixture("description and meta") {
            MonarchSectionHeader(
                title = "Team members",
                description = "People who can sign in to this workspace.",
                meta = "8 members",
                size = MonarchSectionHeaderSize.Md,
            )
        },
        fixture("one action") {
            MonarchSectionHeader(title = "Recent invoices", meta = "12 issued this month") {
                MonarchButton(text = "View all", onClick = {}, size = MonarchButtonSize.Sm)
            }
        },
        fixture("two actions") {
            MonarchSectionHeader(title = "Team members") {
                MonarchButton(text = "Export", onClick = {}, size = MonarchButtonSize.Sm)
                MonarchButton(
                    text = "Invite",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                )
            }
        },
        fixture("busy") { MonarchSectionHeader(title = "Traffic sources", meta = "Last 7 days", busy = true) },
        fixture("long title") {
            MonarchSectionHeader(
                title = "Invoices waiting for a second approval from the finance team before they are sent",
            ) {
                MonarchButton(text = "Review", onClick = {}, size = MonarchButtonSize.Sm)
            }
        },
        fixture("danger tone") {
            MonarchSectionHeader(title = "Overdue", meta = "3 invoices", tone = MonarchTone.Danger)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSectionHeaderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheSecondaryTextFollowsIt() {
        rule.setMonarchContent {
            MonarchSectionHeader(title = "Team members", description = "People who can sign in.", meta = "8 members")
        }
        val title = rule.onNodeWithText("Team members")
        title.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        assertNull(title.stateDescription())
        val description = rule.onNodeWithText("People who can sign in.")
        description.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        val meta = rule.onNodeWithText("8 members")
        assertTrue(title.getUnclippedBoundsInRoot().bottom <= description.getUnclippedBoundsInRoot().top)
        assertTrue(description.getUnclippedBoundsInRoot().bottom <= meta.getUnclippedBoundsInRoot().top)
    }

    @Test
    fun whileBusyTheTitleIsReadWithLoadingAndTheLabelCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                MonarchSectionHeader(title = "Traffic", busy = true)
                MonarchSectionHeader(title = "Sources", busy = true, busyLabel = "Refreshing")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Loading", rule.onNodeWithText("Traffic").stateDescription())
        assertEquals("Refreshing", rule.onNodeWithText("Sources").stateDescription())
    }

    @Test
    fun aShortActionStaysBesideTheTitleAtTheEnd() {
        val environment = MonarchTestEnvironment()
        var clicks = 0
        rule.setMonarchContent(environment) {
            MonarchSectionHeader(title = "Invoices") {
                MonarchButton(text = "View all", onClick = { clicks++ }, size = MonarchButtonSize.Sm)
            }
        }
        val title = rule.onNodeWithText("Invoices", useUnmergedTree = true)
        val action = rule.onNodeWithText("View all")
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            title.assertIsBefore(action, direction)
            assertTrue(action.getUnclippedBoundsInRoot().top < title.getUnclippedBoundsInRoot().bottom, "$direction")
        }
        action.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun wideActionsMoveBelowTheTextAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSectionHeader(title = "Team members") {
                MonarchButton(text = "Export", onClick = {}, size = MonarchButtonSize.Sm)
                MonarchButton(text = "Invite people", onClick = {}, size = MonarchButtonSize.Sm)
            }
        }
        val title = rule.onNodeWithText("Team members", useUnmergedTree = true)
        title.assertTextFullyVisible()
        val action = rule.onNodeWithText("Export")
        assertTrue(action.getUnclippedBoundsInRoot().top >= title.getUnclippedBoundsInRoot().bottom)
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        val text = "People who can sign in to this workspace and see every project in it."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSectionHeader(title = "Team members of the workspace", description = text)
        }
        rule.onNodeWithText("Team members of the workspace", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun actionsStackWhenTheyWouldTakeMoreThanHalfTheWidth() {
        assertFalse(MonarchSectionHeaderTokens.stacksActions(maxWidth = 400, actionsWidth = 150, gap = 12))
        assertTrue(MonarchSectionHeaderTokens.stacksActions(maxWidth = 400, actionsWidth = 200, gap = 12))
        assertFalse(MonarchSectionHeaderTokens.stacksActions(maxWidth = 400, actionsWidth = 0, gap = 12))
        assertFalse(MonarchSectionHeaderTokens.stacksActions(400, 390, 12, bounded = false))
    }

    @Test
    fun theTextUsesTheDefaultAndMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchSectionHeaderTokens.titleColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchSectionHeaderTokens.secondaryColor(colors), theme.id)
        }
        val typography = MonarchTypography.Default
        val sm = MonarchSectionHeaderTokens.titleStyle(MonarchSectionHeaderSize.Sm, typography)
        val md = MonarchSectionHeaderTokens.titleStyle(MonarchSectionHeaderSize.Md, typography)
        assertEquals(typography.label.fontSize, sm.fontSize)
        assertEquals(typography.bodyStrong.fontSize, md.fontSize)
        assertEquals(md.fontWeight, sm.fontWeight)
    }

    @Test
    fun theToneColoursTheTitleWithItsStrongColour() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            fun title(tone: MonarchTone?) = MonarchSectionHeaderTokens.titleColor(colors, tone)
            assertEquals(colors.dangerStrong, title(MonarchTone.Danger), theme.id)
            assertEquals(colors.accentStrong, title(MonarchTone.Accent), theme.id)
            assertEquals(colors.textDefault, title(MonarchTone.Neutral), theme.id)
            assertEquals(colors.textDefault, title(null), theme.id)
        }
    }
}
