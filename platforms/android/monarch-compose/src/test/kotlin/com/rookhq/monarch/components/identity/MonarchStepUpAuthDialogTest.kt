package com.rookhq.monarch.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpAuthDialog
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpAuthDialogLabels
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpAuthDialogTokens
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpCredential
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpMethod
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Short = "Needs your password."

internal object MonarchStepUpAuthDialogFixtures : ComponentFixtures {
    override val id = "step-up-auth-dialog"
    override val fixtures = listOf(
        fixture("password") { MonarchStepUpAuthDialog(onVerify = {}, description = Short) },
        fixture("password with forgot and cancel") {
            MonarchStepUpAuthDialog(
                onVerify = {},
                title = "Confirm",
                description = "Again.",
                onForgot = {},
                onCancel = {},
            )
        },
        fixture("code, with method tabs") {
            MonarchStepUpAuthDialog(
                onVerify = {},
                title = "Confirm",
                description = "Code.",
                methods = listOf(MonarchStepUpMethod.Code, MonarchStepUpMethod.Password),
            )
        },
        fixture("passkey") {
            MonarchStepUpAuthDialog(
                onVerify = {},
                description = "Passkey.",
                methods = listOf(MonarchStepUpMethod.Passkey),
            )
        },
        fixture("device owner") {
            MonarchStepUpAuthDialog(
                onVerify = {},
                description = "Device.",
                methods = listOf(MonarchStepUpMethod.DeviceOwner),
                onRequestBiometric = { true },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStepUpAuthDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theTitleIsAHeadingAndTheDefaultsAreWritten() {
        rule.setMonarchContent { MonarchStepUpAuthDialog(onVerify = {}) }
        rule.onNodeWithText("Confirm it’s you").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("This action needs you to confirm your identity again.").assertExists()
        rule.onNodeWithText("Confirm").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun anEmptyPasswordIsRefusedAndAnnouncedWithoutCallingTheServer() {
        var calls = 0
        rule.setMonarchContent { MonarchStepUpAuthDialog(onVerify = { calls++ }) }
        rule.onNodeWithText("Confirm").performClick()
        assertEquals(0, calls)
        rule.onNodeWithText("Enter your password.", useUnmergedTree = true).assertExists()
        // Announced once, by the password field's own live region (not again by the dialog's).
        rule.onAllNodes(liveRegion and hasContentDescription("Enter your password."), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun thePasswordIsVerifiedAndSuccessCallsOnVerified() {
        var credential: MonarchStepUpCredential? = null
        var verified = 0
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(onVerify = { credential = it }, onVerified = { verified++ })
        }
        rule.onNode(hasSetTextAction()).performTextInput("hunter2")
        rule.onNodeWithText("Confirm").performClick()
        rule.waitForIdle()
        assertEquals(MonarchStepUpCredential.Password("hunter2"), credential)
        assertEquals(1, verified)
    }

    @Test
    fun whileVerifyingTheControlsAreDisabledAndAFailureIsShownOnTheField() {
        val answer = CompletableDeferred<Unit>()
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(
                onVerify = { answer.await() },
                errorMessage = { it.message },
                onCancel = {},
                onVerifyingChange = { changes += it },
            )
        }
        rule.onNode(hasSetTextAction()).performTextInput("wrong")
        rule.onNodeWithText("Confirm").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        answer.completeExceptionally(IllegalStateException("That password is wrong."))
        rule.waitForIdle()
        assertEquals(listOf(true, false), changes)
        rule.onNodeWithText("That password is wrong.", useUnmergedTree = true).assertExists()
        rule.onAllNodes(liveRegion and hasContentDescription("That password is wrong."), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun anIncompleteCodeIsRefusedAndAFullCodeSubmitsItself() {
        var credential: MonarchStepUpCredential? = null
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(onVerify = { credential = it }, methods = listOf(MonarchStepUpMethod.Code))
        }
        rule.onNode(hasSetTextAction()).performTextInput("123")
        rule.onNodeWithText("Confirm").performClick()
        rule.onNodeWithText("Enter all 6 digits.", useUnmergedTree = true).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("456")
        rule.waitForIdle()
        assertEquals(MonarchStepUpCredential.Code("123456"), credential)
    }

    @Test
    fun aFailedPasskeyIsShownInANoticeWithTheGenericMessage() {
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(
                onVerify = { error("refused") },
                methods = listOf(MonarchStepUpMethod.Passkey),
            )
        }
        rule.onNodeWithText("Use passkey").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("That did not work. Try again.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theDeviceCheckRunsFirstAndACancelledCheckChangesNothing() {
        var passes = false
        var credential: MonarchStepUpCredential? = null
        var requests = 0
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(
                onVerify = { credential = it },
                methods = listOf(MonarchStepUpMethod.DeviceOwner),
                onRequestBiometric = {
                    requests++
                    passes
                },
            )
        }
        rule.onNodeWithText("Use screen lock").performClick()
        rule.waitForIdle()
        assertEquals(1, requests)
        assertEquals(null, credential)
        passes = true
        rule.onNodeWithText("Use screen lock").performClick()
        rule.waitForIdle()
        assertEquals(MonarchStepUpCredential.DeviceOwner, credential)
    }

    @Test
    fun theMethodTabsSwitchTheMethodAndCanBeRenamed() {
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(
                onVerify = {},
                methods = listOf(MonarchStepUpMethod.Password, MonarchStepUpMethod.Code),
                labels = MonarchStepUpAuthDialogLabels(
                    methodName = { if (it == MonarchStepUpMethod.Code) "Code" else "Mot de passe" },
                ),
            )
        }
        rule.onNodeWithText("Mot de passe").assertIsSelected()
        rule.onNodeWithText("Code").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Code").assertIsSelected()
        val helper = "Enter the 6-digit code from your authenticator app."
        rule.onNodeWithText(helper, useUnmergedTree = true).assertExists()
    }

    @Test
    fun forgotAndCancelCallBack() {
        var forgot = 0
        var cancelled = 0
        rule.setMonarchContent {
            MonarchStepUpAuthDialog(onVerify = {}, onForgot = { forgot++ }, onCancel = { cancelled++ })
        }
        rule.onNodeWithText("Forgot your password?").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, forgot)
        assertEquals(1, cancelled)
    }

    @Test
    fun theMethodTabsStartAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStepUpAuthDialog(
                onVerify = {},
                methods = listOf(MonarchStepUpMethod.Code, MonarchStepUpMethod.Passkey),
            )
        }
        rule.onNodeWithText("Authenticator app", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Passkey", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStepUpAuthDialog(onVerify = {}, description = "Deleting a workspace needs your password.")
        }
        rule.onNodeWithText("Deleting a workspace needs your password.").assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheMethodsAndTheCode() {
        val tokens = MonarchStepUpAuthDialogTokens
        val all = MonarchStepUpMethod.entries
        assertEquals(all, tokens.methods(all + all, deviceCheck = true))
        assertEquals(all - MonarchStepUpMethod.DeviceOwner, tokens.methods(all, deviceCheck = false))
        assertEquals(listOf(MonarchStepUpMethod.Password), tokens.methods(emptyList(), deviceCheck = true))
        assertEquals(
            listOf(MonarchStepUpMethod.Password),
            tokens.methods(listOf(MonarchStepUpMethod.DeviceOwner), deviceCheck = false),
        )
        assertEquals("123456", tokens.code("12 34-5678"))
        assertTrue(tokens.hasField(MonarchStepUpMethod.Code))
        assertEquals("Password(***)", MonarchStepUpCredential.Password("secret").toString())
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchStepUpAuthDialogTokens.description(colors))
        }
    }
}
