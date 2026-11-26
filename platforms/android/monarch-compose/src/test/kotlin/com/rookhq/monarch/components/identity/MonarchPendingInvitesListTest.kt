@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.memberrow.MonarchMemberRowLabels
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvite
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvitesList
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvitesListLabels
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvitesListTokens
import com.rookhq.monarch.components.layout.divider.MonarchDividerTokens
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_793_750_400_000L
private const val Minute = 60_000L
private const val Hour = 3_600_000L
private const val Day = 24 * Hour
private val Ana = MonarchPendingInvite(
    id = "1",
    email = "ana@example.com",
    role = "Member",
    invitedBy = "Sam Rivera",
    sentAt = Now - 2 * Day,
    expiresAt = Now + 5 * Day,
    link = "https://example.com/invite/1",
)
private val Ben = MonarchPendingInvite(id = "2", email = "ben@example.com", sentAt = Now - 9 * Day, expired = true)

internal object MonarchPendingInvitesListFixtures : ComponentFixtures {
    override val id = "pending-invites-list"
    override val fixtures = listOf(
        fixture("an invitation with every action") {
            MonarchPendingInvitesList(invites = listOf(Ana), now = Now, onResend = {}, onRevoke = {})
        },
        fixture("expired, read-only") { MonarchPendingInvitesList(invites = listOf(Ben), now = Now) },
        fixture("loading") {
            MonarchPendingInvitesList(invites = emptyList(), state = MonarchSectionCardState.Loading)
        },
        fixture("empty") { MonarchPendingInvitesList(invites = emptyList(), now = Now) },
        fixture("error with retry") {
            MonarchPendingInvitesList(invites = emptyList(), state = MonarchSectionCardState.Error, onRetry = {})
        },
        fixture("long email") {
            MonarchPendingInvitesList(
                invites = listOf(Ben.copy(email = "procurement.department.northern.region@example-holdings.com")),
                now = Now,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchPendingInvitesListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachInvitationIsReadAsItsEmailWithStatusRoleAndDetail() {
        rule.setMonarchContent { MonarchPendingInvitesList(invites = listOf(Ana), now = Now) }
        rule.onNodeWithText("Pending invitations").assert(isHeading())
        rule.onNodeWithContentDescription("Pending invitations").assertExists()
        assertEquals(
            "Member, Invited by Sam Rivera · Sent 2 days ago · Expires in 5 days",
            rule.onNodeWithContentDescription("ana@example.com").stateDescription(),
        )
    }

    @Test
    fun anExpiredInvitationSaysSoInWordsAndHasNoCopyLink() {
        rule.setMonarchContent { MonarchPendingInvitesList(invites = listOf(Ben), now = Now, onResend = {}) }
        assertEquals(
            "Expired, Sent last week",
            rule.onNodeWithContentDescription("ben@example.com").stateDescription(),
        )
        rule.onNode(hasText("Copy link")).assertDoesNotExist()
        rule.onNodeWithContentDescription("Resend invite, ben@example.com").assertExists()
    }

    @Test
    fun expiryIsWorkedOutFromTheTime() {
        rule.setMonarchContent {
            MonarchPendingInvitesList(invites = listOf(Ana.copy(expiresAt = Now - Minute)), now = Now)
        }
        assertTrue(rule.onNodeWithContentDescription("ana@example.com").stateDescription()!!.startsWith("Expired"))
    }

    @Test
    fun copyLinkIsNamedAfterTheEmail() {
        rule.setMonarchContent { MonarchPendingInvitesList(invites = listOf(Ana), now = Now) }
        rule.onNodeWithContentDescription("Copy link for ana@example.com")
            .assertTouchTargetAtLeast48dp(rule.density)
            .assert(hasClickAction())
    }

    @Test
    fun resendSendsTheInvitationAgainById() {
        val resent = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchPendingInvitesList(invites = listOf(Ana), now = Now, onResend = { resent += it })
        }
        rule.onNodeWithContentDescription("Resend invite, ana@example.com").performClick()
        rule.waitForIdle()
        assertEquals(listOf("1"), resent)
        rule.onNodeWithContentDescription("Invite sent again.").assertExists()
    }

    @Test
    fun revokeAsksFirstThenRunsAndIsAnnounced() {
        val revoked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchPendingInvitesList(invites = listOf(Ana), now = Now, onRevoke = { revoked += it })
        }
        rule.onNodeWithContentDescription("Revoke invite for ana@example.com").performClick()
        rule.onNodeWithText("Revoke the invite for ana@example.com?").assertExists()
        // The row has its own Revoke button; the dialog's confirm button is the last one.
        rule.onAllNodesWithText("Revoke").onLast().performClick()
        rule.waitForIdle()
        assertEquals(listOf("1"), revoked)
        rule.onNodeWithContentDescription("Invitation to ana@example.com revoked", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun withoutHandlersThereAreNoActions() {
        rule.setMonarchContent { MonarchPendingInvitesList(invites = listOf(Ana.copy(link = null)), now = Now) }
        rule.onNodeWithContentDescription("Resend invite, ana@example.com").assertDoesNotExist()
        rule.onNodeWithContentDescription("Revoke invite for ana@example.com").assertDoesNotExist()
    }

    @Test
    fun aReadyEmptyListShowsTheEmptyState() {
        rule.setMonarchContent { MonarchPendingInvitesList(invites = emptyList(), now = Now) }
        rule.onNodeWithText("No pending invitations").assertExists()
    }

    @Test
    fun anErrorOffersRetry() {
        var retried = 0
        rule.setMonarchContent {
            MonarchPendingInvitesList(
                invites = emptyList(),
                state = MonarchSectionCardState.Error,
                onRetry = { retried++ },
            )
        }
        rule.onNodeWithText("Couldn’t load invitations", substring = true).assertExists()
        rule.onNode(hasText("Try again", substring = true) and hasClickAction()).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPendingInvitesList(
                invites = listOf(Ana),
                now = Now,
                onResend = {},
                labels = MonarchPendingInvitesListLabels(
                    title = "Einladungen",
                    invitedBy = { "Von $it" },
                    sent = { "Gesendet $it" },
                    expires = { "Läuft ab $it" },
                    copyLink = "Link kopieren",
                    copyTarget = { "für $it" },
                    row = MonarchMemberRowLabels(invited = "Eingeladen", resendLabel = { "Erneut senden, $it" }),
                ),
            )
        }
        rule.onNodeWithText("Einladungen").assertExists()
        assertEquals(
            "Member, Von Sam Rivera · Gesendet 2 days ago · Läuft ab in 5 days",
            rule.onNodeWithContentDescription("ana@example.com").stateDescription(),
        )
        rule.onNodeWithContentDescription("Link kopieren für ana@example.com").assertExists()
        rule.onNodeWithContentDescription("Erneut senden, ana@example.com").assertExists()
    }

    @Test
    fun theTitleCanBeReplacedAndExpiredKeepsTheRowStrings() {
        rule.setMonarchContent {
            MonarchPendingInvitesList(
                invites = listOf(Ben),
                now = Now,
                onRevoke = {},
                labels = MonarchPendingInvitesListLabels(
                    expired = "Abgelaufen",
                    row = MonarchMemberRowLabels(revoke = { "Widerrufen $it" }),
                ),
            )
        }
        assertTrue(rule.onNodeWithContentDescription("ben@example.com").stateDescription()!!.startsWith("Abgelaufen"))
        rule.onNodeWithContentDescription("Widerrufen ben@example.com").assertExists()
    }

    @Test
    fun theActionsFollowTheEmailInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPendingInvitesList(invites = listOf(Ana), now = Now, onResend = {})
        }
        // As the web: Copy link comes first, then Resend.
        rule.onNodeWithContentDescription("Copy link for ana@example.com")
            .assertIsBefore(rule.onNodeWithContentDescription("Resend invite, ana@example.com"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPendingInvitesList(invites = listOf(Ana), now = Now, title = "Invitations waiting for an answer")
        }
        rule.onNodeWithText("Invitations waiting for an answer").assertTextFullyVisible()
    }

    @Test
    fun theLogicDecidesStateExpiryDetailAndTimes() {
        val t = MonarchPendingInvitesListTokens
        assertEquals(MonarchSectionCardState.Empty, t.state(MonarchSectionCardState.Ready, 0))
        assertEquals(MonarchSectionCardState.Ready, t.state(MonarchSectionCardState.Ready, 1))
        assertEquals(MonarchSectionCardState.Loading, t.state(MonarchSectionCardState.Loading, 0))
        assertFalse(t.expired(Ana, Now))
        assertTrue(t.expired(Ana, Now + 6 * Day))
        assertTrue(t.expired(Ben, Now))
        assertFalse(t.expired(Ana.copy(expired = false, expiresAt = Now - Day), Now))
        assertFalse(t.expired(Ana.copy(expiresAt = null), Now))
        assertEquals("a · c", t.detail(listOf("a", null, " ", "c")))
        assertNull(t.detail(listOf(null, "")))
        val us = Locale.US
        assertEquals("now", t.relativeTime(Now, Now, us))
        assertEquals("2 days ago", t.relativeTime(Now - 2 * Day, Now, us))
        assertEquals("in 5 days", t.relativeTime(Now + 5 * Day, Now, us))
        assertEquals("in 3 hours", t.relativeTime(Now + 3 * Hour, Now, us))
        assertEquals("10 minutes ago", t.relativeTime(Now - 10 * Minute, Now, us))
        assertEquals("in 2 weeks", t.relativeTime(Now + 14 * Day, Now, us))
        assertEquals("2 months ago", t.relativeTime(Now - 60 * Day, Now, us))
        // The web rule: plus or minus one week, month or year is written in words.
        assertEquals("next year", t.relativeTime(Now + 366 * Day, Now, us))
        assertEquals("in 2 years", t.relativeTime(Now + 731 * Day, Now, us))
    }

    @Test
    fun theDividerUsesTheSubtleBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val variant = MonarchPendingInvitesListTokens.DividerVariant
            assertEquals(c.borderSubtle, MonarchDividerTokens.color(variant, c), theme.id)
        }
    }
}
