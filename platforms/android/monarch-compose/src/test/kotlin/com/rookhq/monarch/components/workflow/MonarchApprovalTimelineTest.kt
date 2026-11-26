package com.rookhq.monarch.components.workflow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalPerson
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalStepStatus
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimeline
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineGlyph
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineLabels
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineStep
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineText
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_772_636_400_000L
private const val Hour = 3_600_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")
private val submitted = MonarchApprovalTimelineStep(
    id = "0",
    label = "Submitted",
    status = MonarchApprovalStepStatus.Submitted,
    actor = MonarchApprovalPerson("Sam Lee"),
    date = Now - 2 * Hour,
)
private val approved = MonarchApprovalTimelineStep(
    id = "1",
    label = "Manager",
    status = MonarchApprovalStepStatus.Approved,
    actor = MonarchApprovalPerson("Ana Ruiz"),
    delegatedFrom = MonarchApprovalPerson("Kim Cho"),
    date = Now - Hour,
    comment = "Fine by me.",
)
private val pending = MonarchApprovalTimelineStep(
    id = "2",
    label = "Finance",
    status = MonarchApprovalStepStatus.Pending,
    approvers = listOf(MonarchApprovalPerson("Jo Park"), MonarchApprovalPerson("Lee Wu")),
    delegatedFrom = MonarchApprovalPerson("Kim Cho"),
)
private val waiting =
    MonarchApprovalTimelineStep(id = "3", label = "Payout", status = MonarchApprovalStepStatus.Waiting)

internal object MonarchApprovalTimelineFixtures : ComponentFixtures {
    override val id = "approval-timeline"
    override val fixtures = listOf(
        fixture("submitted and approved") {
            MonarchApprovalTimeline(steps = listOf(submitted, approved), now = Now, timeZone = Utc.id)
        },
        fixture("pending and waiting") { MonarchApprovalTimeline(steps = listOf(pending, waiting), now = Now) },
        fixture("rejected long ago, no avatars") {
            MonarchApprovalTimeline(
                steps = listOf(
                    MonarchApprovalTimelineStep(
                        id = "r",
                        label = "Legal",
                        status = MonarchApprovalStepStatus.Rejected,
                        actor = MonarchApprovalPerson("Ben Okafor"),
                        date = Now - 30 * 24 * Hour,
                    ),
                ),
                now = Now,
                showAvatars = false,
                timeZone = Utc.id,
            )
        },
        fixture("skipped and cancelled") {
            MonarchApprovalTimeline(
                steps = listOf(
                    MonarchApprovalTimelineStep("s", "Security", MonarchApprovalStepStatus.Skipped, note = "Skip."),
                    MonarchApprovalTimelineStep("c", "Payout", MonarchApprovalStepStatus.Cancelled, note = "Gone."),
                ),
                now = Now,
            )
        },
        fixture("empty") { MonarchApprovalTimeline(steps = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchApprovalTimelineTest {
    @get:Rule
    val rule = createComposeRule()

    private val t = MonarchApprovalTimelineTokens
    private val x = MonarchApprovalTimelineText

    @Test
    fun markersDifferByShapeAsWellAsColour() {
        val colors = MonarchColors(AllThemes.first().colors)
        assertEquals(MonarchApprovalTimelineGlyph.Check, t.marker(MonarchApprovalStepStatus.Approved, colors).shape)
        assertEquals(MonarchApprovalTimelineGlyph.Check, t.marker(MonarchApprovalStepStatus.Submitted, colors).shape)
        assertEquals(MonarchApprovalTimelineGlyph.Cross, t.marker(MonarchApprovalStepStatus.Rejected, colors).shape)
        assertEquals(MonarchApprovalTimelineGlyph.Dot, t.marker(MonarchApprovalStepStatus.Pending, colors).shape)
        assertEquals(MonarchApprovalTimelineGlyph.None, t.marker(MonarchApprovalStepStatus.Waiting, colors).shape)
        assertEquals(MonarchApprovalTimelineGlyph.Dash, t.marker(MonarchApprovalStepStatus.Skipped, colors).shape)
        assertTrue(t.marker(MonarchApprovalStepStatus.Skipped, colors).dashed)
        assertTrue(t.marker(MonarchApprovalStepStatus.Cancelled, colors).dashed)
        assertFalse(t.marker(MonarchApprovalStepStatus.Waiting, colors).dashed)
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.successSolid, t.marker(MonarchApprovalStepStatus.Approved, colors).fill)
            assertEquals(colors.dangerOnSolid, t.marker(MonarchApprovalStepStatus.Rejected, colors).glyph)
            assertEquals(colors.warningSolid, t.marker(MonarchApprovalStepStatus.Pending, colors).border)
            assertEquals(colors.borderStrong, t.marker(MonarchApprovalStepStatus.Waiting, colors).border)
            assertEquals(colors.successBorder, t.rail(MonarchApprovalStepStatus.Submitted, colors))
            assertEquals(colors.borderSubtle, t.rail(MonarchApprovalStepStatus.Pending, colors))
            assertEquals(colors.dangerStrong, t.statusColor(MonarchApprovalStepStatus.Rejected, colors))
            assertEquals(colors.warningStrong, t.statusColor(MonarchApprovalStepStatus.Pending, colors))
            assertEquals(colors.textMuted, t.statusColor(MonarchApprovalStepStatus.Approved, colors))
            assertEquals(colors.textMuted, t.labelColor(MonarchApprovalStepStatus.Skipped, colors))
            assertEquals(colors.textDefault, t.labelColor(MonarchApprovalStepStatus.Rejected, colors))
            assertEquals(colors.bgMuted, t.comment(colors))
        }
    }

    @Test
    fun approversAreListedOnlyOnUndecidedStepsAndDelegationOnDecidedOnes() {
        val approvers: (String) -> String = { "Decided by $it" }
        val delegated: (String) -> String = { "standing in for $it" }
        assertEquals(
            "Decided by Jo Park, Lee Wu (standing in for Kim Cho)",
            x.approversLine(pending, approvers, delegated),
        )
        assertNull(x.approversLine(approved, approvers, delegated))
        assertEquals("standing in for Kim Cho", x.delegationLine(approved, delegated))
        assertNull(x.delegationLine(pending, delegated))
        assertTrue(t.emphasised(MonarchApprovalStepStatus.Pending))
        assertFalse(t.emphasised(MonarchApprovalStepStatus.Approved))
    }

    @Test
    fun timesAreRelativeNearNowAndAbsoluteBeyond() {
        assertEquals("Mar 4, 2026, 3:00\u202FPM", x.absoluteTime(Now, Utc, Locale.US))
        assertEquals("1 hour ago", x.timeText(Now - Hour, Now, t.DefaultAbsoluteAfterMillis, Utc, Locale.US))
        // The shared rule writes the web words, in lower case, where DateUtils wrote "In 2 hours".
        assertEquals("in 2 hours", x.timeText(Now + 2 * Hour, Now, t.DefaultAbsoluteAfterMillis, Utc, Locale.US))
        assertEquals(
            "Feb 2, 2026, 3:00\u202FPM",
            x.timeText(Now - 30 * 24 * Hour, Now, t.DefaultAbsoluteAfterMillis, Utc, Locale.US),
        )
    }

    @Test
    fun eachStepIsOneElementWithItsWholeStory() {
        rule.setMonarchContent {
            MonarchApprovalTimeline(steps = listOf(submitted, approved, pending, waiting), now = Now, timeZone = Utc.id)
        }
        rule.onNodeWithContentDescription("Approval timeline").assertExists()
        rule.onNodeWithContentDescription("Submitted, Submitted, Sam Lee, Mar 4, 2026, 1:00\u202FPM").assertExists()
        rule.onNodeWithContentDescription(
            "Manager, Approved, Ana Ruiz, Mar 4, 2026, 2:00\u202FPM, standing in for Kim Cho, Fine by me.",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "Finance, Waiting for a decision, Current step, Decided by Jo Park, Lee Wu (standing in for Kim Cho)",
        ).assertExists()
        rule.onNodeWithContentDescription("Payout, Up next").assertExists()
        rule.onNodeWithText("1 hour ago", useUnmergedTree = true).assertExists()
    }

    @Test
    fun noStepsShowTheEmptyTextAndLabelsReplaceStrings() {
        rule.setMonarchContent {
            MonarchApprovalTimeline(
                steps = listOf(pending),
                now = Now,
                labels = MonarchApprovalTimelineLabels(
                    label = "Freigabe",
                    status = { "Offen" },
                    approvers = { "Entscheidet: $it" },
                    delegatedFrom = { "für $it" },
                    current = "Aktuell",
                ),
            )
        }
        rule.onNodeWithContentDescription("Freigabe").assertExists()
        rule.onNodeWithContentDescription("Finance, Offen, Aktuell, Entscheidet: Jo Park, Lee Wu (für Kim Cho)")
            .assertExists()
    }

    @Test
    fun theEmptyTextIsShownWithoutSteps() {
        rule.setMonarchContent { MonarchApprovalTimeline(steps = emptyList()) }
        rule.onNodeWithText("Not submitted yet").assertExists()
        rule.onAllNodesWithText("Approval timeline").assertCountEquals(0)
    }

    @Test
    fun theLabelComesBeforeTheStatusInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchApprovalTimeline(steps = listOf(waiting), now = Now) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Payout", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Up next", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchApprovalTimeline(steps = listOf(approved), now = Now, timeZone = Utc.id)
        }
        rule.onNodeWithText("Fine by me.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("standing in for Kim Cho", useUnmergedTree = true).assertTextFullyVisible()
    }
}
