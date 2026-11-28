@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueue
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueueItem
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueueLabels
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueueRejectReason
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueueTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val Hour = 3_600_000L
private const val Now = 1_800_000_000_000L

private val Alex = MonarchApprovalQueueItem(
    id = "alex",
    requesterName = "Alex Morgan",
    title = "Expense claim",
    requesterDetail = "Design",
    summary = "3 receipts, travel",
    submittedMillis = Now - 48 * Hour,
    overdue = true,
    badges = listOf(MonarchStatusDefinition(label = "Resubmitted", tone = MonarchTone.Info)),
)
private val Sam = MonarchApprovalQueueItem(id = "sam", requesterName = "Sam Lee", title = "Leave, 2 days")
private val Jo = MonarchApprovalQueueItem(id = "jo", requesterName = "Jo Park", title = "Timesheet", canDecide = false)

internal object MonarchApprovalQueueFixtures : ComponentFixtures {
    override val id = "approval-queue"
    override val fixtures = listOf(
        fixture("one request with every action") {
            MonarchApprovalQueue(
                items = listOf(Sam),
                now = Now,
                onApprove = {},
                onReject = { _, _ -> },
                onReview = {},
            )
        },
        fixture("overdue, detail, summary and badges") {
            MonarchApprovalQueue(items = listOf(Alex), now = Now, onApprove = {})
        },
        fixture("read-only request") {
            MonarchApprovalQueue(items = listOf(Jo), onApprove = {}, onReject = { _, _ -> }, onReview = {})
        },
        fixture("selectable for bulk approval") {
            MonarchApprovalQueue(items = listOf(Sam, Jo), onApprove = {}, onApproveMany = {})
        },
        fixture("searchable") {
            MonarchApprovalQueue(items = listOf(Sam), searchable = true, onApprove = {})
        },
        fixture("empty") { MonarchApprovalQueue(items = emptyList(), onApprove = {}) },
        fixture("limit with a footer") {
            MonarchApprovalQueue(items = listOf(Sam, Jo), limit = 1) {
                MonarchButton(text = "See all requests", onClick = {}, variant = MonarchButtonVariant.Ghost)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchApprovalQueueTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchApprovalQueueTokens

    private fun announced(text: String) = rule.onNodeWithContentDescription(text)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

    @Test
    fun eachRequestIsOneElementReadInWords() {
        rule.setMonarchContent { MonarchApprovalQueue(items = listOf(Alex), now = Now) }
        val time = tokens.relativeTime(Now - 48 * Hour, Now)
        rule.onNodeWithContentDescription(
            "Alex Morgan, Design, Expense claim, $time, Past due, Resubmitted, 3 receipts, travel",
        ).assertExists()
        rule.onNodeWithContentDescription("Requests waiting for you").assertExists()
        rule.onNodeWithText("Past due", useUnmergedTree = true).assertExists()
    }

    @Test
    fun approveShowsProgressThenAnnouncesTheDecision() {
        val gate = CompletableDeferred<Unit>()
        val approved = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Sam),
                onApprove = {
                    gate.await()
                    approved += it.id
                },
                onReject = { _, _ -> },
            )
        }
        rule.onNodeWithContentDescription("Approve request from Sam Lee").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Send back request from Sam Lee").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Approve request from Sam Lee").performClick()
        assertEquals("Loading", rule.onNodeWithContentDescription("Approve request from Sam Lee").stateDescription())
        rule.onNodeWithContentDescription("Send back request from Sam Lee").assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals(listOf("sam"), approved)
        rule.onNodeWithContentDescription("Send back request from Sam Lee").assertIsEnabled()
        announced("Approved the request from Sam Lee.")
    }

    @Test
    fun aFailedApprovalIsShownOnTheRowAndAnnounced() {
        rule.setMonarchContent {
            MonarchApprovalQueue(items = listOf(Sam), onApprove = { throw IOException() })
        }
        rule.onNodeWithContentDescription("Approve request from Sam Lee").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Sam Lee, Leave, 2 days, That did not work. Try again.").assertExists()
        announced("That did not work. Try again.")
    }

    @Test
    fun sendBackAsksForARequiredReasonAndPassesItTrimmed() {
        var sent: Pair<String, String?>? = null
        rule.setMonarchContent {
            MonarchApprovalQueue(items = listOf(Sam), onReject = { item, reason -> sent = item.id to reason })
        }
        rule.onNodeWithContentDescription("Send back request from Sam Lee").performClick()
        rule.onNode(hasText("Send back the request from Sam Lee?") and hasAnyAncestor(isDialog())).assertExists()
        val confirm = rule.onNode(hasText("Send back") and hasAnyAncestor(isDialog()))
        confirm.assertIsNotEnabled()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("  Missing receipt ")
        confirm.assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals("sam" to "Missing receipt", sent)
        rule.onNode(isDialog()).assertDoesNotExist()
        announced("Sent back the request from Sam Lee.")
    }

    @Test
    fun sendBackWithoutAReasonOnlyConfirms() {
        var reason: String? = "unset"
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Sam),
                rejectReason = MonarchApprovalQueueRejectReason.None,
                onReject = { _, r -> reason = r },
            )
        }
        rule.onNodeWithContentDescription("Send back request from Sam Lee").performClick()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).assertDoesNotExist()
        rule.onNode(hasText("Send back") and hasAnyAncestor(isDialog())).performClick()
        rule.waitForIdle()
        assertNull(reason)
    }

    @Test
    fun aFailedSendBackKeepsTheDialogOpenWithTheError() {
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Sam),
                rejectReason = MonarchApprovalQueueRejectReason.Optional,
                onReject = { _, _ -> throw IOException() },
            )
        }
        rule.onNodeWithContentDescription("Send back request from Sam Lee").performClick()
        rule.onNode(hasText("Send back") and hasAnyAncestor(isDialog())).assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNode(isDialog()).assertExists()
        // The failure is the reason field's error, read with the field.
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "That did not work. Try again.") and
                hasAnyAncestor(isDialog()),
        ).assertExists()
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun aReadOnlyRequestKeepsOnlyReview() {
        var reviewed: String? = null
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Jo),
                onApprove = {},
                onReject = { _, _ -> },
                onReview = { reviewed = it.id },
                onApproveMany = {},
            )
        }
        rule.onNodeWithContentDescription("Approve request from Jo Park").assertDoesNotExist()
        rule.onNodeWithContentDescription("Send back request from Jo Park").assertDoesNotExist()
        rule.onNodeWithContentDescription("Select request from Jo Park").assertIsNotEnabled()
        rule.onNodeWithText("Select all").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Review request from Jo Park").performClick()
        assertEquals("jo", reviewed)
    }

    @Test
    fun selectAllAndBulkApprovalAreConfirmedAndAnnounced() {
        var approved: List<String>? = null
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Sam, Alex, Jo),
                now = Now,
                onApproveMany = { list -> approved = list.map { it.id } },
            )
        }
        rule.onNodeWithContentDescription("Select request from Sam Lee").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Select request from Sam Lee").performClick().assertIsOn()
        rule.onNodeWithText("Select all")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        rule.onNodeWithText("Select all").performClick()
        rule.onNodeWithContentDescription("Select request from Alex Morgan").assertIsOn()
        rule.onNodeWithText("2 of 2 selected", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Approve selected").performClick()
        rule.onNode(hasText("Approve 2 requests?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Approve all") and hasAnyAncestor(isDialog())).performClick()
        rule.waitForIdle()
        assertEquals(listOf("sam", "alex"), approved)
        rule.onNodeWithContentDescription("Select request from Sam Lee").assertIsOff()
        announced("Approved 2 requests.")
    }

    @Test
    fun searchFiltersAndClearFiltersBringsTheRequestsBack() {
        rule.setMonarchContent { MonarchApprovalQueue(items = listOf(Sam, Jo), searchable = true) }
        rule.onNode(hasSetTextAction()).performTextInput("zz")
        rule.onNodeWithText("No requests match your search", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        rule.onNodeWithText("Sam Lee", useUnmergedTree = true).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("jo")
        rule.onNodeWithText("Sam Lee", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Jo Park", useUnmergedTree = true).assertExists()
    }

    @Test
    fun anEmptyQueueSaysSo() {
        rule.setMonarchContent { MonarchApprovalQueue(items = emptyList(), searchable = true, onApproveMany = {}) }
        rule.onNodeWithText("Nothing to review", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Requests that need your decision show up here.", useUnmergedTree = true).assertExists()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        rule.onNodeWithText("Select all").assertDoesNotExist()
    }

    @Test
    fun swipingApprovesTowardTheStartAndReviewsTowardTheEnd() {
        var approved = 0
        var reviewed = 0
        rule.setMonarchContent {
            MonarchApprovalQueue(items = listOf(Sam), onApprove = { approved++ }, onReview = { reviewed++ })
        }
        val row = rule.onNodeWithContentDescription("Sam Lee, Leave, 2 days")
        row.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(1, approved)
        row.performTouchInput { swipeRight() }
        rule.waitForIdle()
        assertEquals(1, reviewed)
    }

    @Test
    fun theCheckboxComesBeforeTheRequestRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchApprovalQueue(items = listOf(Sam), onApproveMany = {})
        }
        rule.onNodeWithContentDescription("Select request from Sam Lee")
            .assertIsBefore(rule.onNodeWithContentDescription("Sam Lee, Leave, 2 days"), LayoutDirection.Rtl)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchApprovalQueue(items = listOf(Alex), now = Now, onApprove = {})
        }
        rule.onNodeWithText("3 receipts, travel", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchApprovalQueue(
                items = listOf(Sam),
                onApprove = {},
                labels = MonarchApprovalQueueLabels(
                    label = "Solicitudes",
                    approve = "Aprobar",
                    approveItem = { "Aprobar a $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Solicitudes").assertExists()
        rule.onNodeWithContentDescription("Aprobar a Sam Lee").assertExists()
        rule.onNodeWithText("Aprobar", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLogicFiltersSelectsAndChecksTheReason() {
        val all = listOf(Sam, Alex, Jo)
        assertEquals(listOf(Alex), tokens.visible(all, " morgan ", null))
        assertEquals(listOf(Sam), tokens.visible(all, "", 1))
        assertEquals(emptyList(), tokens.visible(all, "", -1))
        assertEquals(listOf(Sam), tokens.chosen(all, setOf("sam", "jo", "gone")))
        assertEquals(ToggleableState.Off, tokens.selectAllState(listOf(Jo), setOf("jo")))
        assertEquals(ToggleableState.On, tokens.selectAllState(all, setOf("sam", "alex")))
        assertEquals(setOf("sam", "alex"), tokens.toggleAll(all, setOf("sam")))
        assertEquals(emptySet(), tokens.toggleAll(all, setOf("sam", "alex")))
        assertEquals(false, tokens.canConfirmReject(" ", MonarchApprovalQueueRejectReason.Required))
        assertEquals(true, tokens.canConfirmReject("", MonarchApprovalQueueRejectReason.Optional))
        assertNull(tokens.submittedReason("  ", MonarchApprovalQueueRejectReason.Optional))
        assertNull(tokens.submittedReason("Why", MonarchApprovalQueueRejectReason.None))
        assertEquals("A, C", tokens.spoken(listOf("A", null, " ", "C")))
    }

    @Test
    fun theSwipeColoursComeFromTheSolidRoles() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.successSolid, tokens.swipeColors(approve = true, c).container, theme.id)
            assertEquals(c.successOnSolid, tokens.swipeColors(approve = true, c).content, theme.id)
            assertEquals(c.accentSolid, tokens.swipeColors(approve = false, c).container, theme.id)
            assertEquals(c.accentOnSolid, tokens.swipeColors(approve = false, c).content, theme.id)
        }
    }
}
