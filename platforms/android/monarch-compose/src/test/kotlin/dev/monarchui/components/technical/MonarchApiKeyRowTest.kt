package dev.monarchui.components.technical

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyRow
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyRowLabels
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyRowTokens
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyStatus
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Now = 1_763_100_000_000L
private const val Day = 86_400_000L
private const val Hour = 3_600_000L

internal object MonarchApiKeyRowFixtures : ComponentFixtures {
    override val id = "api-key-row"
    override val fixtures = listOf(
        fixture("active, with actions") {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - 30 * Day,
                lastUsedAt = Now - 2 * Hour,
                scopes = listOf("items:read", "items:write"),
                now = Now,
                onRotate = {},
                onRevoke = {},
            )
        },
        fixture("expiring, more scopes, icon menu") {
            MonarchApiKeyRow(
                name = "Mobile app",
                prefix = "pk_live_8d2c",
                createdAt = Now - 300 * Day,
                expiresAt = Now + 5 * Day,
                scopes = listOf("a:read", "b:read", "c:read", "d:read", "e:read"),
                status = MonarchApiKeyStatus.Expiring,
                now = Now,
                onRotate = {},
                menuIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("expired, never used, no scopes") {
            MonarchApiKeyRow(
                name = "Old CI key",
                prefix = "sk_live_9a01",
                createdAt = Now - 400 * Day,
                expiresAt = Now - 5 * Day,
                status = MonarchApiKeyStatus.Expired,
                now = Now,
                onRevoke = {},
            )
        },
        fixture("revoked, long name and prefix") {
            MonarchApiKeyRow(
                name = "Nightly export to the finance data warehouse",
                prefix = "sk_live_0123456789abcdef0123456789abcdef",
                createdAt = Now - 3 * Day,
                status = MonarchApiKeyStatus.Revoked,
                now = Now,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchApiKeyRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val menu get() = rule.onNodeWithContentDescription("Actions for Reporting job")

    @Test
    fun theRowReadsTheNameStatusMaskedPrefixTimesAndScopes() {
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - 3 * Day,
                lastUsedAt = Now - 2 * Hour,
                expiresAt = Now + 5 * Day,
                scopes = listOf("a", "b", "c", "d", "e"),
                now = Now,
            )
        }
        rule.onNode(hasText("Reporting job") and hasText("Active")).assertExists()
        rule.onNodeWithContentDescription("Key starting with sk_live_4f2a").assertExists()
        rule.onNodeWithText("Created 3 days ago · Last used 2 hours ago · Expires in 5 days", useUnmergedTree = true)
            .assertExists()
        rule.onNodeWithContentDescription("Scopes, a, b, c, d, e").assertExists()
        rule.onNodeWithText("+2", useUnmergedTree = true).assertExists()
        // Without actions there is no menu.
        menu.assertDoesNotExist()
    }

    @Test
    fun rotateAsksFirstThenRunsAndAnnouncesProgress() {
        val gate = CompletableDeferred<Unit>()
        var rotations = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - Day,
                now = Now,
                onRotate = {
                    rotations++
                    gate.await()
                },
                onRevoke = {},
            )
        }
        menu.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Rotate").performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNode(isHeading() and hasText("Rotate Reporting job?")).assertExists()
        assertEquals(0, rotations)
        rule.onNodeWithText("Rotate key").performClick()
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(1, rotations)
        rule.onNode(hasText("Rotating…") and SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertExists()
        rule.onNodeWithText("Rotating…", useUnmergedTree = true).assertTextFullyVisible()
        menu.assertIsNotEnabled()
        gate.complete(Unit)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Rotating…", useUnmergedTree = true).assertDoesNotExist()
        menu.assertIsEnabled()
    }

    @Test
    fun aFailureIsShownWithARetryThatRunsAgain() {
        var attempts = 0
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - Day,
                now = Now,
                onRevoke = {
                    attempts++
                    if (attempts == 1) error("")
                },
            )
        }
        menu.performClick()
        rule.onNodeWithText("Revoke").performClick()
        rule.onNodeWithText("Revoke key").performClick()
        rule.waitForIdle()
        assertEquals(1, attempts)
        rule.onNodeWithText("The key was not revoked.", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(2, attempts)
        rule.onNodeWithText("The key was not revoked.", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun revokeWaitsForThePhraseAndCancelRunsNothing() {
        var revoked = 0
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - Day,
                now = Now,
                revokeConfirmPhrase = "Reporting job",
                onRevoke = { revoked++ },
            )
        }
        menu.performClick()
        rule.onNodeWithText("Revoke").performClick()
        rule.onNodeWithText("Revoke key").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("reporting job")
        rule.onNodeWithText("Revoke key").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(0, revoked)
        menu.performClick()
        rule.onNodeWithText("Revoke").performClick()
        rule.onNode(hasSetTextAction()).performTextInput(" Reporting job ")
        rule.onNodeWithText("Revoke key").assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(1, revoked)
    }

    @Test
    fun expiredAndRevokedKeysHaveNoActions() {
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - Day,
                status = MonarchApiKeyStatus.Revoked,
                now = Now,
                onRotate = {},
                onRevoke = {},
            )
        }
        menu.assertDoesNotExist()
        rule.onNode(hasText("Reporting job") and hasText("Revoked")).assertExists()
        rule.onNodeWithText("Never used", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("No scopes", useUnmergedTree = true).assertExists()
    }

    @Test
    fun extraMenuItemsFollowAndCloseTheMenu() {
        var edits = 0
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - Day,
                now = Now,
            ) { close ->
                MonarchMenuItem(
                    text = "Edit",
                    onClick = {
                        edits++
                        close()
                    },
                )
            }
        }
        menu.performClick()
        rule.onNodeWithText("Edit").performClick()
        rule.waitForIdle()
        assertEquals(1, edits)
        rule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun theMenuIsAtTheEndWith48dpAndTextWrapsAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchApiKeyRow(
                name = "Reporting job",
                prefix = "sk_live_4f2a",
                createdAt = Now - 2 * Day,
                now = Now,
                onRotate = {},
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Reporting job", useUnmergedTree = true).assertIsBefore(menu, direction)
            menu.assertTouchTargetAtLeast48dp(rule.density)
        }
        environment.layoutDirection = LayoutDirection.Ltr
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        rule.onNodeWithText("Created 2 days ago · Never used", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchApiKeyRow(
                name = "Job",
                prefix = "sk_1",
                createdAt = Now - Day,
                now = Now,
                onRotate = {},
                labels = MonarchApiKeyRowLabels(
                    prefix = { "Clé commençant par $it" },
                    created = "Créée",
                    neverUsed = "Jamais utilisée",
                    noScopes = "Aucune portée",
                    status = { "Active (fr)" },
                    menu = "Actions (fr)",
                    actions = { "Actions pour $it" },
                    rotate = "Renouveler",
                    rotateTitle = { "Renouveler $it ?" },
                    rotateConfirm = "Renouveler la clé",
                    cancel = "Annuler",
                ),
            )
        }
        rule.onNodeWithContentDescription("Clé commençant par sk_1").assertExists()
        rule.onNodeWithText("Créée", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jamais utilisée", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Aucune portée", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Job") and hasText("Active (fr)")).assertExists()
        rule.onNodeWithText("Actions (fr)", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Actions pour Job").performClick()
        rule.onNodeWithText("Renouveler").performClick()
        rule.onNodeWithText("Renouveler Job ?", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Renouveler la clé").assertExists()
        rule.onNodeWithText("Annuler").assertExists()
    }

    @Test
    fun theLogicAndColoursComeFromTheTheme() {
        val t = MonarchApiKeyRowTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, t.mutedColor(colors))
        }
        assertEquals(MonarchTone.Success, t.tone(MonarchApiKeyStatus.Active))
        assertEquals(MonarchTone.Warning, t.tone(MonarchApiKeyStatus.Expiring))
        assertEquals(MonarchTone.Neutral, t.tone(MonarchApiKeyStatus.Expired))
        assertEquals(MonarchTone.Danger, t.tone(MonarchApiKeyStatus.Revoked))
        assertTrue(t.live(MonarchApiKeyStatus.Expiring))
        assertFalse(t.live(MonarchApiKeyStatus.Expired))
        assertTrue(t.hasActions(MonarchApiKeyStatus.Active, anyAction = true))
        assertFalse(t.hasActions(MonarchApiKeyStatus.Active, anyAction = false))
        assertFalse(t.hasActions(MonarchApiKeyStatus.Revoked, anyAction = true))
        assertEquals(listOf("a"), t.shownScopes(listOf("a", "b"), 1))
        assertEquals(listOf("a", "b"), t.hiddenScopes(listOf("a", "b"), -1))
        assertTrue(t.phraseMatches(null, ""))
        assertTrue(t.phraseMatches("Job", " Job "))
        assertFalse(t.phraseMatches("Job", "job"))
        assertEquals("3 days ago", t.relativeTime(Now - 3 * Day, Now, Locale.US))
        assertEquals("in 5 days", t.relativeTime(Now + 5 * Day, Now, Locale.US))
        assertEquals("a · b", t.timesLine(listOf("a", "b")))
    }
}
