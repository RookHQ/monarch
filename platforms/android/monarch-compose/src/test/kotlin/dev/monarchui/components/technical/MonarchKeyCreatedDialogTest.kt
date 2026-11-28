package dev.monarchui.components.technical

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.technical.keycreateddialog.MonarchKeyCreatedDialog
import dev.monarchui.components.technical.keycreateddialog.MonarchKeyCreatedDialogKind
import dev.monarchui.components.technical.keycreateddialog.MonarchKeyCreatedDialogLabels
import dev.monarchui.components.technical.keycreateddialog.MonarchKeyCreatedDialogTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.textLayout
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Secret = "sk_live_51Hx9a2Lr8f3"
private const val Acknowledge = "I have saved this API key somewhere safe."
private const val Nudge = "Confirm that you have saved it before you close this dialog."

internal object MonarchKeyCreatedDialogFixtures : ComponentFixtures {
    override val id = "key-created-dialog"
    override val fixtures = listOf(
        fixture("secret") { MonarchKeyCreatedDialog(secret = Secret, onDone = {}) },
        fixture("secret, named, with note") {
            MonarchKeyCreatedDialog(
                secret = Secret,
                onDone = {},
                name = "Production server",
                description = "Use it to call the API from your server.",
                note = "The old key stops working in 24 hours.",
            )
        },
        fixture("masked") { MonarchKeyCreatedDialog(secret = Secret, onDone = {}, masked = true) },
        fixture("multi-line certificate") {
            MonarchKeyCreatedDialog(
                secret = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASC\n-----END PRIVATE KEY-----",
                onDone = {},
                label = "Private key",
                multiline = true,
            )
        },
        fixture("public") {
            MonarchKeyCreatedDialog(
                secret = "pk_live_8Kd02",
                onDone = {},
                kind = MonarchKeyCreatedDialogKind.Public,
                label = "Publishable key",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchKeyCreatedDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private val done get() = rule.onNodeWithText("Done")
    private val checkbox get() = rule.onNode(isToggleable() and hasText(Acknowledge))

    @Test
    fun aSecretCannotCloseUntilAcknowledgedAndDoneExplainsWhy() {
        var closed = 0
        rule.setMonarchContent { MonarchKeyCreatedDialog(secret = Secret, onDone = { closed++ }) }
        rule.onNode(isHeading() and hasText("Copy your new key")).assertExists()
        rule.onNodeWithText("Copy it now. It will not be shown again.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText(Secret, useUnmergedTree = true).assertExists()
        checkbox.assertIsOff()
        done.performClick()
        assertEquals(0, closed)
        val assertive = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)
        rule.onNode(hasText(Nudge) and assertive).assertExists()
        assertEquals(Nudge, checkbox.fetchSemanticsNode().config[SemanticsProperties.Error])
        checkbox.performClick().assertIsOn()
        rule.onNodeWithText(Nudge).assertDoesNotExist()
        done.performClick()
        assertEquals(1, closed)
    }

    @Test
    fun aPublicValueHasAnInformationNoticeAndClosesAtOnce() {
        var closed = 0
        rule.setMonarchContent {
            MonarchKeyCreatedDialog(
                secret = "pk_live_8Kd02",
                onDone = { closed++ },
                kind = MonarchKeyCreatedDialogKind.Public,
                label = "Publishable key",
            )
        }
        rule.onNodeWithText("You can view this again later.", useUnmergedTree = true).assertExists()
        rule.onNode(isToggleable()).assertDoesNotExist()
        done.performClick()
        assertEquals(1, closed)
    }

    @Test
    fun theAcknowledgementCanBeRequiredOrDroppedExplicitly() {
        var closed = 0
        rule.setMonarchContent {
            MonarchKeyCreatedDialog(secret = Secret, onDone = { closed++ }, requiresAcknowledgement = false)
        }
        rule.onNode(isToggleable()).assertDoesNotExist()
        done.performClick()
        assertEquals(1, closed)
    }

    @Test
    fun aNewSecretResetsTheAcknowledgement() {
        var secret by mutableStateOf(Secret)
        rule.setMonarchContent { MonarchKeyCreatedDialog(secret = secret, onDone = {}) }
        checkbox.performClick().assertIsOn()
        secret = "sk_live_other"
        rule.waitForIdle()
        checkbox.assertIsOff()
    }

    @Test
    fun theValueIsCopiedAndNamedAfterItsLabel() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchKeyCreatedDialog(secret = Secret, onDone = {}, label = "Signing secret", onCopy = { copied += it })
        }
        rule.onNodeWithContentDescription("Copy Signing secret").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf(Secret), copied)
        rule.onNode(isToggleable() and hasText("I have saved this Signing secret somewhere safe.")).assertExists()
    }

    @Test
    fun aMaskedValueHidesTheSecretUntilRevealed() {
        rule.setMonarchContent { MonarchKeyCreatedDialog(secret = Secret, onDone = {}, masked = true) }
        val field = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assertEquals("•".repeat(Secret.length), field.textLayout().layoutInput.text.text)
        rule.onNodeWithContentDescription("Show API key").performClick()
        assertEquals(Secret, field.textLayout().layoutInput.text.text)
    }

    @Test
    fun targetsAre48dpAndTextWrapsWholeAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) { MonarchKeyCreatedDialog(secret = Secret, onDone = {}) }
        done.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Copy your new key", useUnmergedTree = true).assertTextFullyVisible()
        environment.fontScale = 1f
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        checkbox.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchKeyCreatedDialog(
                secret = Secret,
                onDone = {},
                labels = MonarchKeyCreatedDialogLabels(
                    title = "Copiez votre clé",
                    label = "Clé API",
                    secretNoticeTitle = "Copiez-la maintenant.",
                    acknowledge = { "J’ai enregistré cette $it." },
                    acknowledgeRequired = "Confirmez d’abord.",
                    done = "Terminé",
                    copy = "Copier",
                ),
            )
        }
        rule.onNodeWithText("Copiez votre clé", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Copiez-la maintenant.", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Copier Clé API").assertExists()
        rule.onNodeWithText("Terminé").performClick()
        rule.onNodeWithText("Confirmez d’abord.").assertExists()
        rule.onNode(isToggleable() and hasText("J’ai enregistré cette Clé API.")).assertExists()
    }

    @Test
    fun theRulesAndColoursComeFromTheTheme() {
        val t = MonarchKeyCreatedDialogTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, t.nudgeColor(colors))
        }
        assertTrue(t.needsAcknowledgement(MonarchKeyCreatedDialogKind.Secret, null))
        assertFalse(t.needsAcknowledgement(MonarchKeyCreatedDialogKind.Public, null))
        assertTrue(t.needsAcknowledgement(MonarchKeyCreatedDialogKind.Public, true))
        assertFalse(t.canClose(needsAcknowledgement = true, acknowledged = false))
        assertTrue(t.canClose(needsAcknowledgement = true, acknowledged = true))
        assertTrue(t.canClose(needsAcknowledgement = false, acknowledged = false))
        assertEquals(MonarchTone.Warning, t.noticeTone(MonarchKeyCreatedDialogKind.Secret))
        assertEquals(MonarchTone.Info, t.noticeTone(MonarchKeyCreatedDialogKind.Public))
        assertTrue(t.sensitive(MonarchKeyCreatedDialogKind.Secret))
        assertFalse(t.sensitive(MonarchKeyCreatedDialogKind.Public))
    }
}
