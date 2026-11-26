package com.rookhq.monarch.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskey
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManager
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManagerLabels
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManagerState
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManagerTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Day = 86_400_000L
private const val Now = 1_800_000_000_000L

private val Synced = MonarchPasskey(
    id = "google",
    name = "Google",
    createdMillis = Now - 3 * Day,
    lastUsedMillis = Now - Day,
    synced = true,
    detail = "Pixel 9",
)
private val Key = MonarchPasskey(id = "key", name = "Security key")

internal object MonarchPasskeyManagerFixtures : ComponentFixtures {
    override val id = "passkey-manager"
    override val fixtures = listOf(
        fixture("a synced passkey with every action") {
            MonarchPasskeyManager(
                passkeys = listOf(Synced),
                description = "",
                now = Now,
                onAdd = { true },
                onRename = { _, _ -> },
                onRemove = {},
                passkeyIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("never used") { MonarchPasskeyManager(passkeys = listOf(Key), description = "", now = Now) },
        fixture("unsupported, empty") {
            MonarchPasskeyManager(passkeys = emptyList(), description = "", supported = false, onAdd = { true })
        },
        fixture("loading") {
            MonarchPasskeyManager(passkeys = emptyList(), state = MonarchPasskeyManagerState.Loading)
        },
        fixture("failed") {
            MonarchPasskeyManager(passkeys = emptyList(), state = MonarchPasskeyManagerState.Failed, onRetry = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPasskeyManagerTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchPasskeyManagerTokens

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun eachPasskeyIsOneElementWithItsDetailsInWords() {
        rule.setMonarchContent { MonarchPasskeyManager(passkeys = listOf(Synced, Key), now = Now) }
        val times = "Added ${tokens.relativeTime(Now - 3 * Day, Now)} · " +
            "Last used ${tokens.relativeTime(Now - Day, Now)}"
        assertEquals("Synced, Pixel 9, $times", rule.onNodeWithContentDescription("Google").stateDescription())
        assertEquals("Never used", rule.onNodeWithContentDescription("Security key").stateDescription())
        rule.onNodeWithContentDescription("Your passkeys").assertExists()
    }

    @Test
    fun addingShowsProgressAndAnnouncesTheOutcome() {
        val gate = CompletableDeferred<Boolean>()
        rule.setMonarchContent { MonarchPasskeyManager(passkeys = emptyList(), onAdd = { gate.await() }) }
        rule.onNodeWithText("Add a passkey").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("Loading", rule.onNodeWithText("Add a passkey").stateDescription())
        gate.complete(true)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Passkey added").assert(live)
    }

    @Test
    fun aCancelledAddIsANeutralNoteAndAFailureIsAnError() {
        var outcome: () -> Boolean = { false }
        rule.setMonarchContent { MonarchPasskeyManager(passkeys = emptyList(), onAdd = { outcome() }) }
        rule.onNodeWithText("Add a passkey").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Passkey setup was cancelled. Nothing was changed.", substring = true).assertExists()
        outcome = { throw IOException() }
        rule.onNodeWithText("Add a passkey").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t add the passkey. Try again.", substring = true).assertExists()
        rule.onNodeWithText("Passkey setup was cancelled", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Couldn’t add the passkey. Try again.").assert(live)
    }

    @Test
    fun anUnsupportedDeviceCannotAddAndSaysWhy() {
        rule.setMonarchContent { MonarchPasskeyManager(passkeys = emptyList(), supported = false, onAdd = { true }) }
        rule.onNodeWithText("Add a passkey").assertIsNotEnabled()
        rule.onNodeWithText("This device can’t create passkeys", substring = true).assertExists()
    }

    @Test
    fun renamingAsksForATrimmedChangedName() {
        val saved = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchPasskeyManager(passkeys = listOf(Key), now = Now, onRename = { id, name -> saved += id to name })
        }
        rule.onNodeWithContentDescription("Rename Security key").performClick()
        val save = rule.onNode(hasText("Save") and hasAnyAncestor(isDialog()))
        save.assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("   ")
        save.assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("  Work key ")
        save.assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(listOf("key" to "Work key"), saved)
        rule.onNodeWithContentDescription("Renamed to Work key").assert(live)
    }

    @Test
    fun removingConfirmsWarnsForTheLastMethodAndBlocksOtherRows() {
        val gate = CompletableDeferred<Unit>()
        rule.setMonarchContent {
            MonarchPasskeyManager(
                passkeys = listOf(Key),
                now = Now,
                lastSignInMethod = true,
                onRename = { _, _ -> },
                onRemove = { gate.await() },
            )
        }
        rule.onNodeWithContentDescription("Remove Security key").performClick()
        rule.onNode(hasText("Remove Security key?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("This is your only way to sign in", substring = true)).assertExists()
        rule.onNode(hasText("Remove passkey") and hasAnyAncestor(isDialog())).performClick()
        assertEquals("Loading", rule.onNodeWithContentDescription("Remove Security key").stateDescription())
        rule.onNodeWithContentDescription("Rename Security key").assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Removed Security key").assert(live)
        rule.onNodeWithContentDescription("Rename Security key").assertIsEnabled()
    }

    @Test
    fun aFailedRemovalShowsANotice() {
        rule.setMonarchContent {
            MonarchPasskeyManager(passkeys = listOf(Key, Synced), now = Now, onRemove = { throw IOException() })
        }
        rule.onNodeWithContentDescription("Remove Security key").performClick()
        rule.onNode(hasText("This is your only way to sign in", substring = true)).assertDoesNotExist()
        rule.onNode(hasText("Remove passkey") and hasAnyAncestor(isDialog())).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t remove Security key. Try again.", substring = true).assertExists()
    }

    @Test
    fun loadingFailedAndEmptyStates() {
        var retries = 0
        rule.setMonarchContent {
            MonarchPasskeyManager(
                passkeys = emptyList(),
                state = MonarchPasskeyManagerState.Failed,
                onRetry = { retries++ },
            )
            MonarchPasskeyManager(passkeys = emptyList(), state = MonarchPasskeyManagerState.Loading)
        }
        rule.onNodeWithText("Couldn’t load your passkeys", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Loading passkeys", substring = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPasskeyManager(
                passkeys = listOf(Key),
                title = "Llaves",
                now = Now,
                onAdd = { true },
                labels = MonarchPasskeyManagerLabels(add = "Añadir", neverUsed = "Nunca usada"),
            )
        }
        rule.onNodeWithText("Llaves", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Añadir").assertExists()
        assertEquals("Nunca usada", rule.onNodeWithContentDescription("Security key").stateDescription())
    }

    @Test
    fun renameComesBeforeRemoveRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPasskeyManager(passkeys = listOf(Key), now = Now, onRename = { _, _ -> }, onRemove = {})
        }
        rule.onNodeWithContentDescription("Rename Security key")
            .assertIsBefore(rule.onNodeWithContentDescription("Remove Security key"), LayoutDirection.Rtl)
    }

    @Test
    fun theHeaderWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPasskeyManager(passkeys = listOf(Key), now = Now, onAdd = { true })
        }
        rule.onNodeWithText("Sign in with your fingerprint", substring = true, useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun namesMustChangeAndTheWarningIsForTheLastPasskeyOnly() {
        assertTrue(tokens.canSave(" New ", "Old"))
        assertFalse(tokens.canSave(" Old ", "Old"))
        assertFalse(tokens.canSave("   ", "Old"))
        assertTrue(tokens.warnsLastMethod(lastSignInMethod = true, count = 1))
        assertFalse(tokens.warnsLastMethod(lastSignInMethod = true, count = 2))
        assertFalse(tokens.warnsLastMethod(lastSignInMethod = false, count = 1))
        assertEquals("A · B", tokens.line("A", null, "B"))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, tokens.iconColor(c), theme.id)
            assertEquals(c.dangerStrong, tokens.warningColor(c), theme.id)
        }
    }
}
