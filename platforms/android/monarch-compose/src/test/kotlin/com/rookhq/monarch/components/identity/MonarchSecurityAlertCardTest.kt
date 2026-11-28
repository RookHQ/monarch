package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlert
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCard
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCardLabels
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCardState
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCardTokens
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertSeverity
import com.rookhq.monarch.foundation.MonarchTone
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Now = 1_767_225_600_000L
private const val FiveMinutes = 5 * 60_000L

private val SignIn = MonarchSecurityAlert(
    id = "1",
    title = "New sign-in",
    description = "Lisbon",
    severity = MonarchSecurityAlertSeverity.High,
    dateMillis = Now - FiveMinutes,
    actionLabel = "Review",
)
private val Password = MonarchSecurityAlert(id = "2", title = "Password changed", read = true)

internal object MonarchSecurityAlertCardFixtures : ComponentFixtures {
    override val id = "security-alert-card"
    override val fixtures = listOf(
        fixture("unread with follow-up and mark as read") {
            MonarchSecurityAlertCard(alerts = listOf(SignIn), now = Now, onAction = {}, onMarkRead = {})
        },
        fixture("mark all and dismiss") {
            MonarchSecurityAlertCard(
                alerts = listOf(SignIn.copy(description = null, actionLabel = null)),
                now = Now,
                onMarkAllRead = {},
                onDismiss = {},
            )
        },
        fixture("read alerts of every severity") {
            MonarchSecurityAlertCard(
                alerts = MonarchSecurityAlertSeverity.entries.take(2).map {
                    MonarchSecurityAlert(id = it.name, title = "Alert ${it.name}", severity = it, read = true)
                },
            )
        },
        fixture("critical") {
            MonarchSecurityAlertCard(
                alerts = listOf(
                    MonarchSecurityAlert(
                        id = "c",
                        title = "Recovery codes used",
                        severity = MonarchSecurityAlertSeverity.Critical,
                    ),
                ),
            )
        },
        fixture("loading") {
            MonarchSecurityAlertCard(alerts = emptyList(), state = MonarchSecurityAlertCardState.Loading)
        },
        fixture("empty") { MonarchSecurityAlertCard(alerts = emptyList()) },
        fixture("failed") {
            MonarchSecurityAlertCard(alerts = emptyList(), state = MonarchSecurityAlertCardState.Failed, onRetry = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSecurityAlertCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndUnreadIsCounted() {
        rule.setMonarchContent { MonarchSecurityAlertCard(alerts = listOf(SignIn, Password), now = Now) }
        rule.onNodeWithText("Security alerts").assert(isHeading())
        rule.onNodeWithText("1 unread", useUnmergedTree = true).assertExists()
    }

    @Test
    fun eachAlertIsReadWithItsStateSeverityAndTime() {
        rule.setMonarchContent { MonarchSecurityAlertCard(alerts = listOf(SignIn, Password), now = Now) }
        val alert = rule.onNodeWithContentDescription("New sign-in, Lisbon")
        assertEquals("Unread, High, 5 minutes ago", alert.stateDescription())
        assertEquals("Info", rule.onNodeWithContentDescription("Password changed").stateDescription())
    }

    @Test
    fun theActionsRunWithTheAlert() {
        var acted: String? = null
        var read: String? = null
        var allRead = 0
        rule.setMonarchContent {
            MonarchSecurityAlertCard(
                alerts = listOf(SignIn),
                now = Now,
                onAction = { acted = it.id },
                onMarkRead = { read = it },
                onMarkAllRead = { allRead++ },
            )
        }
        rule.onNodeWithText("Review").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Mark as read").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Mark all as read").performClick()
        assertEquals("1", acted)
        assertEquals("1", read)
        assertEquals(1, allRead)
    }

    @Test
    fun aReadAlertHasNoMarkAsReadAndAnAlertWithoutHandlerNoFollowUp() {
        rule.setMonarchContent { MonarchSecurityAlertCard(alerts = listOf(SignIn.copy(read = true)), onMarkRead = {}) }
        rule.onAllNodesWithText("Mark as read").assertCountEquals(0)
        rule.onAllNodesWithText("Review").assertCountEquals(0)
    }

    @Test
    fun dismissingIsNamedWithTheTitleAndAnnounced() {
        rule.setMonarchContent {
            var alerts by remember { mutableStateOf(listOf(SignIn, Password)) }
            MonarchSecurityAlertCard(
                alerts = alerts,
                now = Now,
                onDismiss = { id -> alerts = alerts.filterNot { it.id == id } },
            )
        }
        rule.onNodeWithContentDescription("Dismiss New sign-in")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onAllNodesWithContentDescription("New sign-in, Lisbon").assertCountEquals(0)
        rule.onNodeWithContentDescription("Dismissed New sign-in")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun emptyAndFailedUseTheSharedStates() {
        var state by mutableStateOf(MonarchSecurityAlertCardState.Ready)
        var retries = 0
        rule.setMonarchContent {
            MonarchSecurityAlertCard(alerts = emptyList(), state = state, onRetry = { retries++ })
        }
        rule.onNodeWithText("No security alerts", useUnmergedTree = true).assertExists()
        state = MonarchSecurityAlertCardState.Failed
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t load security alerts", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loadingShowsAProgressIndicatorWithText() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchSecurityAlertCard(alerts = emptyList(), state = MonarchSecurityAlertCardState.Loading)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Loading alerts").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSecurityAlertCard(
                alerts = listOf(SignIn),
                now = Now,
                labels = MonarchSecurityAlertCardLabels(
                    title = "Alertas",
                    severity = { "Alta" },
                    unread = "Sin leer",
                    unreadCount = { "$it sin leer" },
                ),
            )
        }
        rule.onNodeWithText("Alertas").assertExists()
        rule.onNodeWithText("1 sin leer", useUnmergedTree = true).assertExists()
        assertEquals(
            "Sin leer, Alta, 5 minutes ago",
            rule.onNodeWithContentDescription("New sign-in, Lisbon").stateDescription(),
        )
    }

    @Test
    fun theUnreadCountSitsUnderTheTitleRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSecurityAlertCard(alerts = listOf(SignIn), now = Now)
        }
        val title = rule.onNodeWithText("Security alerts").getUnclippedBoundsInRoot()
        val unread = rule.onNodeWithText("1 unread", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(title.top < unread.top)
    }

    @Test
    fun titlesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSecurityAlertCard(alerts = listOf(SignIn), now = Now)
        }
        rule.onNodeWithText("New sign-in", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theSpokenTextJoinsTheParts() {
        val t = MonarchSecurityAlertCardTokens
        assertEquals("High, now", t.spokenState(null, "High", "now"))
        assertEquals("Title", t.spokenName("Title", " "))
        assertEquals(1, t.unreadCount(listOf(SignIn, Password)))
        assertEquals(MonarchBadgeVariant.Solid, t.badgeVariant(MonarchSecurityAlertSeverity.Critical))
        assertEquals(MonarchTone.Neutral, t.tone(MonarchSecurityAlertSeverity.Low))
        assertEquals(MonarchTone.Warning, t.tone(MonarchSecurityAlertSeverity.Medium))
    }

    @Test
    fun theColoursComeFromTheSeverityRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchSecurityAlertCardTokens
            assertEquals(c.infoStrong, t.iconColor(MonarchSecurityAlertSeverity.Info, c), theme.id)
            assertEquals(c.textMuted, t.iconColor(MonarchSecurityAlertSeverity.Low, c), theme.id)
            assertEquals(c.warningStrong, t.iconColor(MonarchSecurityAlertSeverity.Medium, c), theme.id)
            assertEquals(c.dangerStrong, t.iconColor(MonarchSecurityAlertSeverity.Critical, c), theme.id)
            assertEquals(c.accentSolid, t.unreadColor(read = false, c), theme.id)
            assertEquals(Color.Transparent, t.unreadColor(read = true, c), theme.id)
        }
    }
}
