package dev.monarchui.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.memberrow.MonarchMemberRow
import dev.monarchui.components.identity.memberrow.MonarchMemberRowLabels
import dev.monarchui.components.identity.memberrow.MonarchMemberRowNotice
import dev.monarchui.components.identity.memberrow.MonarchMemberRowStatus
import dev.monarchui.components.identity.memberrow.MonarchMemberRowTokens
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.components.identity.rolepicker.MonarchRoleOption
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
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Roles = listOf(
    MonarchRoleOption("admin", "Admin"),
    MonarchRoleOption("member", "Member"),
    MonarchRoleOption("owner", "Owner", enabled = false),
)

internal object MonarchMemberRowFixtures : ComponentFixtures {
    override val id = "member-row"
    override val fixtures = listOf(
        fixture("self with presence and read-only role") {
            MonarchMemberRow(
                name = "Priya Raman",
                email = "priya@example.com",
                presence = MonarchPresenceStatus.Online,
                role = "admin",
                roles = Roles,
                self = true,
            )
        },
        fixture("editable role and remove") {
            MonarchMemberRow(
                name = "Jonas Berg",
                email = "jonas@example.com",
                role = "member",
                roles = Roles,
                badges = listOf("Guest"),
                onRoleChange = {},
                onRemove = {},
            )
        },
        fixture("invited") {
            MonarchMemberRow(
                name = "ana@example.com",
                meta = "Invite expires in 3 days",
                status = MonarchMemberRowStatus.Invited,
                onResend = {},
                onRemove = {},
            )
        },
        fixture("suspended, disabled, long name") {
            MonarchMemberRow(
                name = "Maximiliane Alexandra von Hohenberg-Schwarzenstein",
                email = "maximiliane.alexandra.von.hohenberg@example-enterprise.com",
                status = MonarchMemberRowStatus.Suspended,
                enabled = false,
                onRemove = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMemberRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theIdentityIsOneElementWithItsDetailsAsState() {
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "Priya Raman",
                email = "priya@example.com",
                meta = "Joined March 2024",
                presence = MonarchPresenceStatus.Away,
                status = MonarchMemberRowStatus.Suspended,
                role = "admin",
                roles = Roles,
                badges = listOf("Guest"),
                self = true,
            )
        }
        val identity = rule.onNodeWithContentDescription("Priya Raman, You")
        assertEquals(
            "Suspended, Away, Guest, Admin, priya@example.com, Joined March 2024",
            identity.stateDescription(),
        )
        // Self hides Remove by default even though nothing could remove anyway.
        rule.onNodeWithContentDescription("Remove Priya Raman").assertDoesNotExist()
    }

    @Test
    fun removeAsksFirstThenRunsAndShowsAFailure() {
        var removed = 0
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "Jonas Berg",
                onRemove = {
                    removed++
                    error("Only owners can remove members")
                },
                removeConsequences = listOf("3 tickets go back to the queue"),
            )
        }
        rule.onNodeWithContentDescription("Remove Jonas Berg").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Remove Jonas Berg?").assertExists()
        rule.onNodeWithText("3 tickets go back to the queue").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, removed)
        rule.onNodeWithContentDescription("Remove Jonas Berg").performClick()
        rule.onAllNodesWithText("Remove").onLast().performClick()
        rule.waitForIdle()
        assertEquals(1, removed)
        rule.onNodeWithContentDescription("Only owners can remove members")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anInvitationOffersResendAndRevoke() {
        var resent = 0
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "ana@example.com",
                status = MonarchMemberRowStatus.Invited,
                onResend = { resent++ },
                onRemove = {},
            )
        }
        assertEquals("Invited", rule.onNodeWithContentDescription("ana@example.com").stateDescription())
        rule.onNodeWithContentDescription("Revoke invite for ana@example.com").assertExists()
        rule.onNodeWithContentDescription("Resend invite, ana@example.com")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(1, resent)
        rule.onNodeWithContentDescription("Invite sent again.").assertExists()
    }

    @Test
    fun swipeActionsAreAlsoTalkBackActionsOnTheMember() {
        var resent = 0
        var removed = 0
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "ana@example.com",
                status = MonarchMemberRowStatus.Invited,
                onResend = { resent++ },
                onRemove = { removed++ },
                swipeActions = true,
            )
        }
        val member = rule.onNodeWithContentDescription("ana@example.com")
        val actions = member.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(
            listOf("Revoke invite for ana@example.com", "Resend invite, ana@example.com"),
            actions.map { it.label },
        )
        rule.runOnIdle { actions[1].action() }
        rule.waitForIdle()
        assertEquals(1, resent)
        // Remove still asks first.
        rule.runOnIdle { actions[0].action() }
        rule.onNode(isDialog()).assertExists()
        assertEquals(0, removed)
        rule.onAllNodesWithText("Revoke invite").filterToOne(hasAnyAncestor(isDialog())).performClick()
        rule.waitForIdle()
        assertEquals(1, removed)
    }

    @Test
    fun withoutSwipeActionsTheMemberHasNoTalkBackActionsAndTheStatusToneCanChange() {
        rule.setMonarchContent {
            MonarchMemberRow(name = "Lee", status = MonarchMemberRowStatus.Suspended, onRemove = {})
        }
        val config = rule.onNodeWithContentDescription("Lee").fetchSemanticsNode().config
        assertEquals(null, config.getOrNull(SemanticsActions.CustomActions))
        val tokens = MonarchMemberRowTokens
        assertEquals(MonarchTone.Warning, tokens.statusTone(MonarchMemberRowStatus.Suspended, null))
        assertEquals(MonarchTone.Info, tokens.statusTone(MonarchMemberRowStatus.Invited, null))
        assertEquals(MonarchTone.Danger, tokens.statusTone(MonarchMemberRowStatus.Suspended, MonarchTone.Danger))
    }

    @Test
    fun aRoleChangeShowsTheNewRoleAndRestoresItOnFailure() {
        val gate = CompletableDeferred<Unit>()
        val asked = mutableListOf<String>()
        var role by mutableStateOf("member")
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "Jonas Berg",
                role = role,
                roles = Roles,
                onRoleChange = {
                    asked += it
                    gate.await()
                    error("No")
                },
            )
        }
        rule.onNodeWithText("Member").performClick()
        rule.onNodeWithText("Admin").performClick()
        rule.waitForIdle()
        assertEquals(listOf("admin"), asked)
        rule.onNodeWithText("Admin").assertExists().assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithText("Member").assertExists()
        rule.onNodeWithContentDescription("No").assertExists()
    }

    @Test
    fun disabledRowDisablesItsControls() {
        rule.setMonarchContent {
            MonarchMemberRow(name = "Jonas Berg", enabled = false, onRemove = {})
        }
        rule.onNodeWithContentDescription("Remove Jonas Berg").assertIsNotEnabled()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchMemberRow(
                name = "Jonas Berg",
                status = MonarchMemberRowStatus.Invited,
                onRemove = {},
                labels = MonarchMemberRowLabels(invited = "Invité", revoke = { "Révoquer $it" }),
            )
        }
        rule.onNodeWithContentDescription("Révoquer Jonas Berg").assertExists()
        assertEquals("Invité", rule.onNodeWithContentDescription("Jonas Berg").stateDescription())
    }

    @Test
    fun controlsFollowTheReadingDirectionAndFitAtLargeFonts() {
        val environment = MonarchTestEnvironment(layoutDirection = LayoutDirection.Rtl)
        rule.setMonarchContent(environment) {
            MonarchMemberRow(
                name = "ana@example.com",
                status = MonarchMemberRowStatus.Invited,
                onResend = {},
                onRemove = {},
            )
        }
        rule.onNodeWithText("Resend invite")
            .assertIsBefore(rule.onNodeWithContentDescription("Revoke invite for ana@example.com"), LayoutDirection.Rtl)
        // At the largest font size the buttons wrap onto their own lines and keep their words whole.
        environment.fontScale = LargestFontScale
        rule.onNodeWithText("Resend invite").assertTextFullyVisible()
        rule.onNodeWithContentDescription("Revoke invite for ana@example.com").assertIsDisplayed()
    }

    @Test
    fun logicDecidesControlsTitlesFailuresAndColours() {
        val tokens = MonarchMemberRowTokens
        assertTrue(tokens.editable(null, hasCallback = true, self = false, roleCount = 2))
        assertFalse(tokens.editable(null, hasCallback = true, self = true, roleCount = 2))
        assertTrue(tokens.editable(true, hasCallback = true, self = true, roleCount = 2))
        assertFalse(tokens.editable(true, hasCallback = false, self = false, roleCount = 2))
        assertFalse(tokens.removable(null, hasCallback = true, self = true))
        assertEquals("Admin", tokens.roleTitle("admin", Roles))
        assertEquals("guest", tokens.roleTitle("guest", Roles))
        assertEquals("Fallback", tokens.failureText(IllegalStateException(" "), "Fallback"))
        assertEquals("a, b", tokens.spokenValue(listOf("a", null, "", "b")))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.nameColor(MonarchMemberRowStatus.Suspended, colors))
            assertEquals(colors.textDefault, tokens.nameColor(MonarchMemberRowStatus.Active, colors))
            assertEquals(colors.dangerStrong, tokens.noticeColor(MonarchMemberRowNotice("x", error = true), colors))
            assertEquals(colors.successStrong, tokens.noticeColor(MonarchMemberRowNotice("x", error = false), colors))
        }
    }

    @Test
    fun labelsCanBeCopiedWithAFewChanged() {
        val base = MonarchMemberRowLabels(you = "You", cancel = "Cancel")
        val copy = base.copy(cancel = "Keep")
        assertEquals("You", copy.you)
        assertEquals("Keep", copy.cancel)
        assertNull(copy.invited)
    }
}
