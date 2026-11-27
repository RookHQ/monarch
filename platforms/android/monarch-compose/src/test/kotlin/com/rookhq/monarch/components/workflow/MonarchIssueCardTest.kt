@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.issuecard.MonarchIssueCard
import com.rookhq.monarch.components.workflow.issuecard.MonarchIssueCardAssignee
import com.rookhq.monarch.components.workflow.issuecard.MonarchIssueCardLabels
import com.rookhq.monarch.components.workflow.issuecard.MonarchIssueCardTokens
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityLevel
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Todo = MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted)
private val Doing = MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started)
private val Done = MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed)
private val Dropped = MonarchStatusOption("dropped", "Won't do", MonarchStatusCategory.Cancelled)
private val IssueStatuses = listOf(Todo, Doing, Done)

/** 10, 11 and 12 March 2026 at midnight UTC. */
private const val March10 = 1_773_100_800_000L
private const val March11 = 1_773_187_200_000L
private const val March12 = 1_773_273_600_000L
private const val HourMillis = 3_600_000L

internal object MonarchIssueCardFixtures : ComponentFixtures {
    override val id = "issue-card"
    override val fixtures = listOf(
        fixture("editable status and priority") {
            MonarchIssueCard(
                title = "Sign-in fails on slow networks",
                key = "STR-142",
                status = Doing,
                statuses = IssueStatuses,
                priority = MonarchPriorityLevel.High,
                assignee = MonarchIssueCardAssignee.Person("Ana Ruiz"),
                onStatusChange = {},
                onPriorityChange = {},
                onOpen = {},
            )
        },
        fixture("read-only, tags and overdue") {
            MonarchIssueCard(
                title = "Fix the export",
                key = "STR-7",
                status = Todo,
                priority = MonarchPriorityLevel.Urgent,
                tags = listOf("Bug", "Auth"),
                dueDate = March10,
                today = March12,
            )
        },
        fixture("selected and unassigned") {
            MonarchIssueCard(
                title = "Write the release notes",
                key = "STR-150",
                assignee = MonarchIssueCardAssignee.Unassigned,
                selected = true,
                onOpen = {},
            )
        },
        fixture("dragging, done") {
            MonarchIssueCard(title = "Ship it", key = "STR-1", status = Done, dueDate = March10, dragging = true)
        },
        fixture("long title, title only") {
            MonarchIssueCard(
                title = "Waiting for a second approval from finance before the vendor can be paid this month",
                key = "FIN-2048",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchIssueCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isImage = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image)
    private val isDropdown = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)

    @Test
    fun theTitleIsAButtonReadWithTheKeyFirst() {
        var opened = 0
        rule.setMonarchContent { MonarchIssueCard(title = "Sign-in fails", key = "STR-142", onOpen = { opened++ }) }
        rule.onNodeWithContentDescription("STR-142, Sign-in fails")
            .assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutOnOpenTheTitleIsNotActionable() {
        rule.setMonarchContent { MonarchIssueCard(title = "Sign-in fails", key = "STR-142") }
        rule.onNodeWithContentDescription("STR-142, Sign-in fails")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun aSelectedCardReadsItsTitleAsSelected() {
        rule.setMonarchContent {
            Column {
                MonarchIssueCard(title = "A", key = "K-1", selected = true)
                MonarchIssueCard(title = "B", key = "K-2")
            }
        }
        rule.onNodeWithContentDescription("K-1, A").assertIsSelected()
        rule.onNodeWithContentDescription("K-2, B").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }

    @Test
    fun aReadOnlyStatusAndPriorityAreReadWithTheirNames() {
        rule.setMonarchContent {
            MonarchIssueCard(title = "T", key = "K", status = Doing, priority = MonarchPriorityLevel.High)
        }
        rule.onNodeWithContentDescription(
            "Status: In progress",
        ).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        rule.onNodeWithContentDescription("Priority: High").assert(isImage)
    }

    @Test
    fun anEditableStatusIsAPickerThatCallsBackWithTheId() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchIssueCard(
                title = "T",
                key = "K",
                status = Doing,
                statuses = IssueStatuses,
                onStatusChange = { chosen = it },
            )
        }
        rule.onNodeWithContentDescription("Status: In progress").assert(isDropdown).performClick()
        rule.onNode(hasText("In progress") and hasAnyAncestor(isPopup()) and !isHeading()).assertIsSelected()
        rule.onNode(hasText("Done") and hasAnyAncestor(isPopup())).assertIsNotSelected().performClick()
        assertEquals("done", chosen)
    }

    @Test
    fun anEditableStatusWithoutStatusesStaysReadOnly() {
        rule.setMonarchContent { MonarchIssueCard(title = "T", key = "K", status = Doing, onStatusChange = {}) }
        rule.onNodeWithContentDescription(
            "Status: In progress",
        ).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun anEditablePriorityIsAPickerWith48dpAndRenamedLevels() {
        var chosen: MonarchPriorityLevel? = null
        rule.setMonarchContent {
            MonarchIssueCard(
                title = "T",
                key = "K",
                priority = MonarchPriorityLevel.Low,
                labels = MonarchIssueCardLabels(
                    priorityLevel = { if (it == MonarchPriorityLevel.Low) "Baja" else it.name },
                ),
                onPriorityChange = { chosen = it },
            )
        }
        rule.onNodeWithContentDescription("Priority: Baja")
            .assert(isDropdown)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Urgent") and hasAnyAncestor(isPopup())).performClick()
        assertEquals(MonarchPriorityLevel.Urgent, chosen)
    }

    @Test
    fun aPassedDueDayIsWrittenOverdue() {
        rule.setMonarchContent {
            MonarchIssueCard(title = "T", key = "K", status = Todo, dueDate = March10, today = March12)
        }
        rule.onNodeWithContentDescription("Due Mar 10, Overdue").assertExists()
    }

    @Test
    fun closedIssuesAndTodayAreNotOverdue() {
        rule.setMonarchContent {
            Column {
                MonarchIssueCard(title = "A", key = "A", status = Done, dueDate = March10, today = March12)
                MonarchIssueCard(
                    title = "B",
                    key = "B",
                    dueDate = March12 + HourMillis,
                    today = March12 + 20 * HourMillis,
                )
            }
        }
        rule.onNodeWithContentDescription("Due Mar 10").assertExists()
        rule.onNodeWithContentDescription("Due Mar 12").assertExists()
        rule.onAllNodes(hasContentDescription("Overdue", substring = true)).assertCountEquals(0)
    }

    @Test
    fun overdueComparesDaysAndSkipsClosedStatuses() {
        val today = MonarchIssueCardTokens.day(March12)
        assertTrue(MonarchIssueCardTokens.isOverdue(March11, today, Todo))
        assertTrue(MonarchIssueCardTokens.isOverdue(March11 + 23 * HourMillis, today, null))
        assertFalse(MonarchIssueCardTokens.isOverdue(March12, today, Doing))
        assertFalse(MonarchIssueCardTokens.isOverdue(March10, today, Done))
        assertFalse(MonarchIssueCardTokens.isOverdue(March10, today, Dropped))
    }

    @Test
    fun todayIsTheDeviceDayAndBeforeEpochDaysRoundDown() {
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        // 20:00 UTC on 11 March is already 12 March in Tokyo.
        assertEquals(
            MonarchIssueCardTokens.day(March12),
            MonarchIssueCardTokens.today(March11 + 20 * HourMillis, tokyo),
        )
        assertEquals(-1L, MonarchIssueCardTokens.day(-1L))
    }

    @Test
    fun theDueDayIsFormattedInUtcAndTheGivenLocale() {
        assertEquals("Mar 12", MonarchIssueCardTokens.formatDue(March12, Locale.US))
        assertEquals("Mar 12", MonarchIssueCardTokens.formatDue(March12 + 23 * HourMillis, Locale.US))
    }

    @Test
    fun tagsAndAssigneeAreRead() {
        rule.setMonarchContent {
            Column {
                MonarchIssueCard(
                    title = "A",
                    key = "A",
                    tags = listOf("Bug", "Auth"),
                    assignee = MonarchIssueCardAssignee.Person("Ana Ruiz"),
                )
                MonarchIssueCard(title = "B", key = "B", assignee = MonarchIssueCardAssignee.Unassigned)
            }
        }
        rule.onNodeWithContentDescription("Labels: Bug, Auth").assertExists()
        rule.onNodeWithContentDescription("Assigned to Ana Ruiz").assert(isImage)
        rule.onNodeWithContentDescription("Unassigned").assert(isImage)
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchIssueCard(
                title = "T",
                key = "K",
                status = Todo,
                tags = listOf("Bug"),
                dueDate = March10,
                today = March12,
                assignee = MonarchIssueCardAssignee.Unassigned,
                labels = MonarchIssueCardLabels(
                    due = { "Vence $it" },
                    overdue = "Vencida",
                    tags = { "Etiquetas: $it" },
                    unassigned = "Sin asignar",
                    status = { "Estado: $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Vence Mar 10, Vencida").assertExists()
        rule.onNodeWithContentDescription("Etiquetas: Bug").assertExists()
        rule.onNodeWithContentDescription("Sin asignar").assertExists()
        rule.onNodeWithContentDescription("Estado: Todo").assertExists()
    }

    @Test
    fun theStatusComesBeforeTheAssigneeRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchIssueCard(title = "T", key = "K", status = Todo, assignee = MonarchIssueCardAssignee.Person("Ana"))
        }
        rule.onNodeWithContentDescription("Status: Todo")
            .assertIsBefore(rule.onNodeWithContentDescription("Assigned to Ana"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchIssueCard(title = "Sign-in fails on slow networks", key = "STR-142", onOpen = {})
        }
        rule.onNode(hasText("Sign-in fails on slow networks"), useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun surfaceColoursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val plain = MonarchIssueCardTokens.surface(false, colors)
            val chosen = MonarchIssueCardTokens.surface(true, colors)
            assertEquals(colors.bgSurface, plain.container)
            assertEquals(colors.borderDefault, plain.border)
            assertEquals(colors.accentSubtle, chosen.container)
            assertEquals(colors.accentSolid, chosen.border)
            assertTrue(chosen.borderWidth > plain.borderWidth)
            assertEquals(colors.dangerStrong, MonarchIssueCardTokens.dueColor(true, colors))
            assertEquals(colors.textMuted, MonarchIssueCardTokens.dueColor(false, colors))
            assertEquals(colors.borderStrong, MonarchIssueCardTokens.unassignedColor(colors))
        }
    }
}
