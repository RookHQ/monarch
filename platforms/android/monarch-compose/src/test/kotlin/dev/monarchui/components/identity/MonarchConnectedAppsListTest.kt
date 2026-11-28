package dev.monarchui.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedApp
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsList
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsListLabels
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsListState
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsListTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Hour = 3_600_000L
private const val Now = 1_800_000_000_000L
private const val LongScope = "See, edit, create and delete every document in the shared drives you can open"

private val Notes = MonarchConnectedApp(
    id = "notes",
    name = "Notes",
    domain = "notes.example.com",
    verified = true,
    workspace = "Acme",
    scopes = listOf("See your email address", "See your name"),
    connectedAtMillis = Now - 72 * Hour,
    lastUsedAtMillis = Now - 2 * Hour,
)
private val Sheets = MonarchConnectedApp(id = "sheets", name = "Sheets", suspended = true)

private fun MonarchConnectedApp.withOneScope() = MonarchConnectedApp(
    id = id,
    name = name,
    domain = domain,
    verified = verified,
    workspace = workspace,
    scopes = scopes.take(1),
    connectedAtMillis = connectedAtMillis,
    lastUsedAtMillis = lastUsedAtMillis,
)

internal object MonarchConnectedAppsListFixtures : ComponentFixtures {
    override val id = "connected-apps-list"
    override val fixtures = listOf(
        fixture("one app with everything") {
            MonarchConnectedAppsList(
                apps = listOf(Notes.withOneScope()),
                title = "",
                now = Now,
                onRevoke = {},
            )
        },
        fixture("suspended app, no revoke") { MonarchConnectedAppsList(apps = listOf(Sheets), now = Now) },
        fixture("loading") {
            MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Loading)
        },
        fixture("error with retry") {
            MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Error, onRetry = {})
        },
        fixture("empty") { MonarchConnectedAppsList(apps = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchConnectedAppsListTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchConnectedAppsListTokens

    @Test
    fun eachAppIsOneElementWithItsDetailsInWords() {
        rule.setMonarchContent { MonarchConnectedAppsList(apps = listOf(Notes, Sheets), now = Now) }
        rule.onNodeWithText("Connected apps", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Connected apps").assertExists()
        val times = "Connected ${tokens.relativeTime(Notes.connectedAtMillis!!, Now)} · " +
            "Last used ${tokens.relativeTime(Notes.lastUsedAtMillis!!, Now)}"
        assertEquals(
            "Verified, notes.example.com, $times, Workspace: Acme",
            rule.onNodeWithContentDescription("Notes").stateDescription(),
        )
        assertEquals("Suspended", rule.onNodeWithContentDescription("Sheets").stateDescription())
        rule.onNodeWithContentDescription("What Notes can access").assertExists()
        rule.onNodeWithText("• See your email address").assertExists()
    }

    @Test
    fun removingAsksFirstShowsProgressAndAnnounces() {
        val gate = CompletableDeferred<Unit>()
        val revoked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchConnectedAppsList(
                apps = listOf(Notes),
                now = Now,
                onRevoke = {
                    gate.await()
                    revoked += it.id
                },
            )
        }
        rule.onNodeWithContentDescription("Remove access for Notes")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNode(hasText("Remove access for Notes?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Remove access") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals("Loading", rule.onNodeWithContentDescription("Remove access for Notes").stateDescription())
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals(listOf("notes"), revoked)
        rule.onNodeWithContentDescription("Removed access for Notes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun cancellingTheConfirmationRemovesNothing() {
        var calls = 0
        rule.setMonarchContent { MonarchConnectedAppsList(apps = listOf(Notes), now = Now, onRevoke = { calls++ }) }
        rule.onNodeWithContentDescription("Remove access for Notes").performClick()
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(0, calls)
    }

    @Test
    fun aFailureIsShownUnderTheRowAndAnnounced() {
        rule.setMonarchContent {
            MonarchConnectedAppsList(
                apps = listOf(Notes, Sheets),
                now = Now,
                confirmRevoke = false,
                onRevoke = { if (it.id == "notes") throw IOException("The network is down") else throw IOException() },
            )
        }
        rule.onNodeWithContentDescription("Remove access for Notes").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The network is down").assertExists()
        rule.onNodeWithContentDescription("Remove access for Sheets").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t remove access for Sheets. Try again.").assertExists()
        rule.onNodeWithContentDescription("Couldn’t remove access for Sheets. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun loadingErrorAndEmptyStates() {
        var retries = 0
        rule.setMonarchContent {
            MonarchConnectedAppsList(
                apps = emptyList(),
                state = MonarchConnectedAppsListState.Error,
                onRetry = { retries++ },
            )
        }
        rule.onNodeWithText("Couldn’t load your connected apps", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theLoadingAndEmptyStatesSaySo() {
        rule.setMonarchContent {
            MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Loading)
            MonarchConnectedAppsList(apps = emptyList())
        }
        rule.onNodeWithContentDescription("Loading connected apps", substring = true).assertExists()
        rule.onNodeWithText("No connected apps", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplacedAndAnEmptyTitleHidesTheHeader() {
        rule.setMonarchContent {
            MonarchConnectedAppsList(
                apps = listOf(Notes),
                title = "",
                now = Now,
                onRevoke = {},
                labels = MonarchConnectedAppsListLabels(
                    revoke = "Quitar acceso",
                    revokeLabel = { "Quitar acceso a $it" },
                    verified = "Verificada",
                ),
            )
        }
        rule.onNodeWithText("Connected apps", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Quitar acceso a Notes").assertExists()
        assertTrue(rule.onNodeWithContentDescription("Notes").stateDescription()!!.startsWith("Verificada"))
    }

    @Test
    fun rowsStartAtTheRightRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchConnectedAppsList(apps = listOf(MonarchConnectedApp(id = "d", name = "Drive")), now = Now)
        }
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val name = rule.onNodeWithContentDescription("Drive").fetchSemanticsNode().boundsInRoot
        assertTrue(name.right > root.width / 2)
    }

    @Test
    fun scopesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchConnectedAppsList(
                apps = listOf(MonarchConnectedApp(id = "a", name = "Drive", scopes = listOf(LongScope))),
                title = "",
            )
        }
        rule.onNodeWithText("• $LongScope").assertTextFullyVisible()
    }

    @Test
    fun linesAreBuiltFromThePartsThatExist() {
        assertEquals(null, tokens.timesLine(null, null))
        assertEquals("A · B", tokens.timesLine("A", "B"))
        assertEquals("B", tokens.timesLine(null, "B"))
        assertEquals(null, tokens.labelled("Workspace", null))
        assertEquals("Workspace Acme", tokens.labelled("Workspace", "Acme"))
        assertEquals("Verified, a.com", tokens.spokenDetails(listOf("Verified"), "a.com", null, null))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, tokens.nameColor(suspended = true, c), theme.id)
            assertEquals(c.textDefault, tokens.nameColor(suspended = false, c), theme.id)
            assertEquals(c.dangerStrong, tokens.failureColor(c), theme.id)
        }
    }
}
