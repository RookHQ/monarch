package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardLabels
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchSectionCardFixtures : ComponentFixtures {
    override val id = "section-card"
    override val fixtures = listOf(
        fixture("ready with actions and footer") {
            MonarchSectionCard(
                title = "Open tickets",
                meta = "12 open",
                actions = { MonarchButton(text = "View all", onClick = {}, variant = MonarchButtonVariant.Ghost) },
                footer = { MonarchText(text = "Updated 5 min ago") },
            ) { MonarchText(text = "Printer offline on floor 3") }
        },
        fixture("loading") {
            MonarchSectionCard(title = "Revenue", state = MonarchSectionCardState.Loading) {}
        },
        fixture("empty") {
            MonarchSectionCard(title = "Invoices", state = MonarchSectionCardState.Empty, emptyMessage = "None yet.") {}
        },
        fixture("error with retry") {
            MonarchSectionCard(title = "Usage", state = MonarchSectionCardState.Error, onRetry = {}) {}
        },
        fixture("busy and flush") {
            MonarchSectionCard(title = "Members", busy = true, flush = true) { MonarchText(text = "Ada Lovelace") }
        },
        fixture("danger tone with icon") {
            MonarchSectionCard(
                title = "Danger zone",
                description = "Deleting the workspace removes every project.",
                tone = MonarchTone.Danger,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            ) {
                MonarchButton(text = "Delete workspace", onClick = {}, variant = MonarchButtonVariant.Danger)
            }
        },
        fixture("collapsible, collapsed") {
            MonarchSectionCard(
                title = "Advanced",
                expanded = false,
                onExpandedChange = {},
                toggleIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            ) { MonarchText(text = "Hidden") }
        },
        fixture("collapsible, expanded, long title") {
            MonarchSectionCard(
                title = "Notification settings for every project in this workspace",
                expanded = true,
                onExpandedChange = {},
            ) { MonarchText(text = "Shown") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSectionCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun theHeaderIsOneHeadingAndTheActionsStaySeparate() {
        var views = 0
        rule.setMonarchContent {
            MonarchSectionCard(
                title = "Open tickets",
                description = "Assigned to you",
                meta = "12 open",
                actions = { MonarchButton(text = "View all", onClick = { views++ }, size = MonarchButtonSize.Sm) },
            ) { MonarchText(text = "Printer offline") }
        }
        rule.onNode(hasText("Open tickets") and isHeading)
            .assert(hasText("Assigned to you") and hasText("12 open"))
        assertNull(rule.onNode(hasText("Open tickets") and isHeading).stateDescription())
        rule.onNode(hasText("View all") and hasClickAction()).performClick()
        assertEquals(1, views)
        rule.onNodeWithText("Printer offline").assertExists()
    }

    @Test
    fun aToneIsSpokenWithTheTitleAndColoursTheBorder() {
        rule.setMonarchContent {
            MonarchSectionCard(title = "Danger zone", tone = MonarchTone.Danger) { MonarchText(text = "Body") }
        }
        assertEquals("Danger", rule.onNode(hasText("Danger zone") and isHeading).stateDescription())
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.borderDefault, MonarchSectionCardTokens.border(MonarchTone.Neutral, colors))
            assertEquals(colors.dangerBorder, MonarchSectionCardTokens.border(MonarchTone.Danger, colors))
            assertEquals(colors.warningStrong, MonarchSectionCardTokens.iconColor(MonarchTone.Warning, colors))
            assertEquals(colors.textMuted, MonarchSectionCardTokens.iconColor(MonarchTone.Neutral, colors))
            assertNotEquals(MonarchSectionCardTokens.container(colors), colors.dangerBorder, theme.id)
        }
        assertNull(MonarchSectionCardTokens.toneName(MonarchTone.Neutral))
    }

    @Test
    fun aCollapsibleHeaderIsAButtonThatShowsAndHidesTheBodyAndFooter() {
        var expanded by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchSectionCard(
                title = "Advanced",
                expanded = expanded,
                onExpandedChange = { expanded = it },
                footer = { MonarchText(text = "Footer") },
            ) { MonarchText(text = "Body") }
        }
        val header = rule.onNode(hasText("Advanced") and isButton)
        assertEquals("Expanded", header.stateDescription())
        header.assertTouchTargetAtLeast48dp(rule.density)
        header.performClick()
        assertEquals(false, expanded)
        rule.onNodeWithText("Body").assertDoesNotExist()
        rule.onNodeWithText("Footer").assertDoesNotExist()
        assertEquals("Collapsed", header.stateDescription())
        header.performSemanticsAction(SemanticsActions.Expand)
        assertEquals(true, expanded)
        rule.onNodeWithText("Body").assertExists()
        rule.onNodeWithText("Footer").assertExists()
    }

    @Test
    fun withoutOnExpandedChangeTheCardIsNotCollapsible() {
        rule.setMonarchContent {
            MonarchSectionCard(title = "Members", expanded = false) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Body").assertExists()
        rule.onNode(hasText("Members") and isButton).assertDoesNotExist()
    }

    @Test
    fun loadingShowsSkeletonRowsAndSaysLoading() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchSectionCard(title = "Revenue", state = MonarchSectionCardState.Loading) {
                MonarchText(text = "Body")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Body").assertDoesNotExist()
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertEquals("Loading", rule.onNode(hasText("Revenue") and isHeading).stateDescription())
    }

    @Test
    fun theEmptyAndErrorStatesReplaceTheBody() {
        var state by mutableStateOf(MonarchSectionCardState.Empty)
        var retries = 0
        rule.setMonarchContent {
            MonarchSectionCard(
                title = "Usage",
                state = state,
                emptyMessage = "No usage this month.",
                errorMessage = "The server did not answer.",
                onRetry = { retries++ },
            ) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Nothing here yet", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("No usage this month.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Body").assertDoesNotExist()
        state = MonarchSectionCardState.Error
        rule.waitForIdle()
        rule.onNodeWithText("This section didn’t load", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("The server did not answer.", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Try again") and hasClickAction()).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun aFlushBodyRunsToTheEdges() {
        rule.setMonarchContent {
            Column {
                MonarchSectionCard(title = "Flush", modifier = Modifier.testTag("flush card"), flush = true) {
                    MonarchText(text = "Edge", modifier = Modifier.testTag("flush body"))
                }
                MonarchSectionCard(title = "Padded", modifier = Modifier.testTag("padded card")) {
                    MonarchText(text = "Inset", modifier = Modifier.testTag("padded body"))
                }
            }
        }
        val flushCard = rule.onNodeWithTag("flush card").getUnclippedBoundsInRoot()
        val flushBody = rule.onNodeWithTag("flush body", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(flushCard.left, flushBody.left)
        val paddedCard = rule.onNodeWithTag("padded card").getUnclippedBoundsInRoot()
        val paddedBody = rule.onNodeWithTag("padded body", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(paddedBody.left > paddedCard.left)
        assertEquals(MonarchSectionCardTokens.bodyPadding(flush = true).calculateBottomPadding().value, 0f)
    }

    @Test
    fun theActionsFollowTheTitleInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSectionCard(
                title = "Tickets",
                actions = { MonarchButton(text = "Add", onClick = {}, size = MonarchButtonSize.Sm) },
            ) {}
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Tickets", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Add", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun aLongTitleWrapsWholeAtTheLargestFontScale() {
        val title = "Notification settings for every project in this workspace"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSectionCard(title = title, expanded = true, onExpandedChange = {}) {}
        }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        // The busy ring turns for ever, so the clock is advanced by hand.
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchSectionCard(
                title = "Zone",
                tone = MonarchTone.Warning,
                busy = true,
                state = MonarchSectionCardState.Empty,
                expanded = true,
                onExpandedChange = {},
                labels = MonarchSectionCardLabels(
                    loading = "Chargement",
                    emptyTitle = "Rien",
                    expanded = "Ouvert",
                    tone = "Attention",
                ),
            ) {}
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Ouvert, Attention, Chargement", rule.onNode(hasText("Zone") and isButton).stateDescription())
        rule.onNodeWithText("Rien", useUnmergedTree = true).assertExists()
    }
}
