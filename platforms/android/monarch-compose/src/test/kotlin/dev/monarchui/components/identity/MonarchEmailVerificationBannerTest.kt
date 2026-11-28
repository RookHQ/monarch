package dev.monarchui.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import dev.monarchui.components.identity.emailverificationbanner.MonarchEmailVerificationBanner
import dev.monarchui.components.identity.emailverificationbanner.MonarchEmailVerificationBannerLabels
import dev.monarchui.components.identity.emailverificationbanner.MonarchEmailVerificationBannerTokens
import dev.monarchui.components.identity.emailverificationbanner.MonarchResendCooldown
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Message =
    "We sent a verification link to ana@example.com. " + "Open it to confirm the address belongs to you."

internal object MonarchEmailVerificationBannerFixtures : ComponentFixtures {
    override val id = "email-verification-banner"
    override val fixtures = listOf(
        fixture("warning, ready") { MonarchEmailVerificationBanner(
            email = "ana@example.com",
            onResend = {},
            dismissIcon = null,
        ) },
        fixture("info with change") {
            MonarchEmailVerificationBanner(
                email = "ana@example.com",
                onResend = {},
                tone = MonarchTone.Info,
                onChangeEmail = {},
                dismissIcon = null,
            )
        },
        fixture("dismissible") {
            MonarchEmailVerificationBanner(
                email = "ana@example.com",
                onResend = {},
                onDismiss = {},
                labels = MonarchEmailVerificationBannerLabels(message = { "Open the link we sent to $it." }),
                dismissIcon = null,
            )
        },
        fixture("cooldown, page strip") {
            MonarchEmailVerificationBanner(
                email = "a.very.long.address.for.someone@subsidiary.example.com",
                onResend = {},
                placement = MonarchCalloutBannerPlacement.Page,
                initialCooldownSeconds = 42,
                dismissIcon = null,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEmailVerificationBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleAndMessageWithTheAddressAreOneElementWithTheTone() {
        rule.setMonarchContent { MonarchEmailVerificationBanner(
            email = "ana@example.com",
            onResend = {},
            dismissIcon = null,
        ) }
        val merged = rule.onNodeWithText(Message).fetchSemanticsNode()
        val texts = merged.config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf("Warning", "Verify your email address", Message), texts)
    }

    @Test
    fun aSendShowsSentAndStartsTheCooldown() {
        var sends = 0
        rule.setMonarchContent {
            MonarchEmailVerificationBanner(
                email = "ana@example.com",
                onResend = { sends++ },
                cooldownSeconds = 3,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Resend email").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1, sends)
        rule.onNodeWithContentDescription("Verification email sent")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Resend in 3s").assertIsNotEnabled()
        rule.mainClock.advanceTimeBy(1_100)
        rule.onNodeWithText("Resend in 2s").assertExists()
        rule.mainClock.advanceTimeBy(2_500)
        rule.onNodeWithText("Resend email").assertIsEnabled()
    }

    @Test
    fun aFailedSendIsShownAndResendIsAvailableAtOnce() {
        rule.setMonarchContent {
            MonarchEmailVerificationBanner(
                email = "ana@example.com",
                onResend = { throw IOException("offline") },
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Resend email").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Couldn’t send the email. Try again.").assertExists()
        rule.onNodeWithText("Resend email").assertIsEnabled()
    }

    @Test
    fun theInitialCooldownDisablesResend() {
        var sends = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchEmailVerificationBanner(
                email = "a@b.c",
                onResend = { sends++ },
                initialCooldownSeconds = 10,
                dismissIcon = null,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Resend in 10s").assertIsNotEnabled().performClick()
        assertEquals(0, sends)
    }

    @Test
    fun changeAndDismissAreSeparateButtons() {
        var changes = 0
        var dismissals = 0
        rule.setMonarchContent {
            MonarchEmailVerificationBanner(
                email = "a@b.c",
                onResend = {},
                onChangeEmail = { changes++ },
                onDismiss = { dismissals++ },
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Change email").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Dismiss").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, changes)
        assertEquals(1, dismissals)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchEmailVerificationBanner(
                email = "a@b.c",
                onResend = {},
                labels = MonarchEmailVerificationBannerLabels(
                    title = "Verifica tu correo",
                    message = { "Enviamos un enlace a $it." },
                    resend = "Reenviar",
                ),
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Verifica tu correo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Enviamos un enlace a a@b.c.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Reenviar").assertExists()
    }

    @Test
    fun resendComesBeforeChangeRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEmailVerificationBanner(email = "a@b.c", onResend = {}, onChangeEmail = {}, dismissIcon = null)
        }
        rule.onNodeWithText("Resend email").assertIsBefore(rule.onNodeWithText("Change email"), LayoutDirection.Rtl)
    }

    @Test
    fun theMessageWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEmailVerificationBanner(email = "ana@example.com", onResend = {}, dismissIcon = null)
        }
        rule.onNodeWithText(Message, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theCooldownLogic() {
        val ready = MonarchResendCooldown.start(0)
        assertTrue(ready.canSend)
        val sending = ready.begin()!!
        assertNull(sending.begin())
        val sent = sending.succeed(cooldown = 2)
        assertTrue(sent.sent)
        assertFalse(sent.canSend)
        assertEquals(0, sent.tick().tick().tick().remaining)
        val failed = sent.tick().tick().begin()!!.fail()
        assertTrue(failed.failed)
        assertFalse(failed.sent)
        assertTrue(failed.canSend)
        assertEquals(0, MonarchResendCooldown.start(-4).remaining)
    }

    @Test
    fun theToneAndColoursComeFromTheRolesInEveryTheme() {
        val t = MonarchEmailVerificationBannerTokens
        assertEquals(MonarchTone.Info, t.tone(MonarchTone.Info))
        assertEquals(MonarchTone.Warning, t.tone(MonarchTone.Danger))
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, t.statusColor(failed = true, c), theme.id)
            assertEquals(c.successStrong, t.statusColor(failed = false, c), theme.id)
        }
    }
}
