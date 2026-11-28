@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.identity

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.identity.devicelist.MonarchDeviceKind
import dev.monarchui.components.identity.sessionlist.MonarchSession
import dev.monarchui.components.identity.sessionlist.MonarchSessionList
import dev.monarchui.components.identity.sessionlist.MonarchSessionListLabels
import dev.monarchui.components.identity.sessionlist.MonarchSessionListState
import dev.monarchui.components.identity.sessionlist.MonarchSessionListTokens
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardState
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Hour = 3_600_000L
private const val Now = 1_800_000_000_000L

private val Phone = MonarchSession(
    id = "phone",
    label = "Rook app on Android",
    kind = MonarchDeviceKind.Mobile,
    detail = "Lisbon",
    signedInMillis = Now - 50 * Hour,
    lastActiveMillis = Now,
    current = true,
)
private val Mac = MonarchSession(
    id = "mac",
    label = "Chrome on macOS",
    detail = "Porto",
    signedInMillis = Now - 50 * Hour,
    lastActiveMillis = Now - 5 * Hour,
)
private val Tablet = MonarchSession(id = "tab", label = "Safari on iPad", kind = MonarchDeviceKind.Tablet)

internal object MonarchSessionListFixtures : ComponentFixtures {
    override val id = "session-list"
    override val fixtures = listOf(
        fixture("current session with sign out") {
            MonarchSessionList(sessions = listOf(Phone), description = "", now = Now, onSignOut = {})
        },
        fixture("other session, icon and sign out others") {
            MonarchSessionList(
                sessions = listOf(Mac),
                title = "Sessions",
                description = "",
                now = Now,
                onSignOut = {},
                onSignOutOthers = {},
                sessionIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("no actions, with a footnote") {
            MonarchSessionList(sessions = listOf(Tablet), description = "", footnote = "Signed in for 30 days.")
        },
        fixture("loading") { MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Loading) },
        fixture("failed") {
            MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Failed, onRetry = {})
        },
        fixture("empty") { MonarchSessionList(sessions = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSessionListTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchSessionListTokens

    @Test
    fun eachSessionIsOneElementWithItsDetailsInWords() {
        rule.setMonarchContent { MonarchSessionList(sessions = listOf(Phone, Mac), now = Now) }
        val signedIn = "Signed in ${tokens.relativeTime(Now - 50 * Hour, Now)}"
        // The current session leaves out its last-active time: it is in use now.
        assertEquals(
            "This device, Lisbon, $signedIn",
            rule.onNodeWithContentDescription("Rook app on Android").stateDescription(),
        )
        assertEquals(
            "Porto, $signedIn · Last active ${tokens.relativeTime(Now - 5 * Hour, Now)}",
            rule.onNodeWithContentDescription("Chrome on macOS").stateDescription(),
        )
        rule.onNodeWithContentDescription("Signed-in sessions").assertExists()
    }

    @Test
    fun signOutOthersShowsOnlyWithOtherSessions() {
        rule.setMonarchContent { MonarchSessionList(sessions = listOf(Phone), now = Now, onSignOutOthers = {}) }
        rule.onNodeWithText("Sign out other devices").assertDoesNotExist()
        assertEquals(listOf("mac", "tab"), tokens.otherIds(listOf(Phone, Mac, Tablet)))
        assertEquals(true, tokens.showsSignOutOthers(handled = true, ready = true, listOf(Phone, Mac)))
        assertEquals(false, tokens.showsSignOutOthers(handled = false, ready = true, listOf(Phone, Mac)))
        assertEquals(false, tokens.showsSignOutOthers(handled = true, ready = false, listOf(Phone, Mac)))
    }

    @Test
    fun signingOutOneAsksFirstAndOnlyOneRunsAtATime() {
        val gate = CompletableDeferred<Unit>()
        val signedOut = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchSessionList(
                sessions = listOf(Phone, Mac),
                now = Now,
                onSignOut = {
                    gate.await()
                    signedOut += it
                },
            )
        }
        rule.onNodeWithContentDescription("Sign out Chrome on macOS").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Sign out Chrome on macOS").performClick()
        rule.onNode(hasText("Sign out Chrome on macOS?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("That device will need to sign in again.") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals("Loading", rule.onNodeWithContentDescription("Sign out Chrome on macOS").stateDescription())
        rule.onNodeWithContentDescription("Sign out Rook app on Android").assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals(listOf("mac"), signedOut)
        rule.onNodeWithContentDescription("Sign out Rook app on Android").assertIsEnabled()
        rule.onNodeWithContentDescription("Signed out Chrome on macOS")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theCurrentSessionHasItsOwnConfirmation() {
        rule.setMonarchContent { MonarchSessionList(sessions = listOf(Phone), now = Now, onSignOut = {}) }
        rule.onNodeWithContentDescription("Sign out Rook app on Android").performClick()
        rule.onNode(hasText("Sign out of this device?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun signingOutOthersPassesTheirIdsAndAnnouncesTheCount() {
        var ids: List<String>? = null
        rule.setMonarchContent {
            MonarchSessionList(sessions = listOf(Phone, Mac, Tablet), now = Now, onSignOutOthers = { ids = it })
        }
        rule.onNodeWithText("Sign out other devices").performClick()
        rule.onNode(hasText("Sign out other devices?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("2 other devices", substring = true) and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Sign out other devices") and hasAnyAncestor(isDialog())).performClick()
        rule.waitForIdle()
        assertEquals(listOf("mac", "tab"), ids)
        rule.onNodeWithContentDescription("Signed out 2 other devices").assertExists()
    }

    @Test
    fun aFailedSignOutShowsANoticeAndIsAnnounced() {
        rule.setMonarchContent {
            MonarchSessionList(
                sessions = listOf(Mac),
                now = Now,
                confirm = false,
                onSignOut = { throw IOException() },
            )
        }
        rule.onNodeWithContentDescription("Sign out Chrome on macOS").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t sign out Chrome on macOS. Try again.", substring = true).assertExists()
        rule.onNodeWithContentDescription("Couldn’t sign out Chrome on macOS. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theLoadingFailedAndEmptyStatesSaySo() {
        var retries = 0
        rule.setMonarchContent {
            Column {
                MonarchSessionList(
                    sessions = emptyList(),
                    state = MonarchSessionListState.Failed,
                    onRetry = { retries++ },
                )
                MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Loading)
                MonarchSessionList(sessions = emptyList())
            }
        }
        rule.onNodeWithText("Couldn’t load your sessions", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
        rule.onNodeWithContentDescription("Loading sessions", substring = true).assertExists()
        rule.onNodeWithText("No active sessions", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSessionList(
                sessions = listOf(Phone),
                title = "Sesiones",
                now = Now,
                onSignOut = {},
                labels = MonarchSessionListLabels(
                    current = "Este dispositivo",
                    signOut = "Cerrar sesión",
                    signOutLabel = { "Cerrar $it" },
                ),
            )
        }
        rule.onNodeWithText("Sesiones", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Cerrar Rook app on Android").assertExists()
        assertEquals(
            true,
            rule.onNodeWithContentDescription("Rook app on Android").stateDescription()?.startsWith("Este dispositivo"),
        )
    }

    @Test
    fun theTitleComesBeforeSignOutOthersRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSessionList(sessions = listOf(Phone, Mac), title = "Sessions", now = Now, onSignOutOthers = {})
        }
        rule.onNodeWithText("Sessions", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Sign out other devices", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSessionList(sessions = listOf(Mac), now = Now, onSignOut = {})
        }
        rule.onNodeWithText("Devices and browsers", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun linesAreBuiltFromThePartsThatExist() {
        assertEquals(null, tokens.line(null, " "))
        assertEquals("A · C", tokens.line("A", null, "C"))
        assertEquals("This device, A", tokens.spokenDetails("This device", "A", null))
        assertEquals("", tokens.spokenDetails(null, null, null))
        assertEquals(MonarchSectionCardState.Empty, tokens.cardState(MonarchSessionListState.Ready, empty = true))
        assertEquals(MonarchSectionCardState.Error, tokens.cardState(MonarchSessionListState.Failed, empty = true))
        assertEquals(MonarchSectionCardState.Ready, tokens.cardState(MonarchSessionListState.Ready, empty = false))
    }
}
