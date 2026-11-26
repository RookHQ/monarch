package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copyfield.MonarchCopyField
import com.rookhq.monarch.components.forms.otppininput.MonarchOtpPinInput
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCode
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCodes
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.stepper.MonarchStepper
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperStep
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of TwoFactorSetup: a recipe of existing parts. */
object TwoFactorSetupSection : CataloguePage {
    override val title = "TwoFactorSetup"

    @Composable
    override fun Content() {
        CatalogueCaption("Three steps: connect, verify, save the codes")
        val secret = "JBSWY3DPEHPK3PXP"
        val otpauth = "otpauth://totp/Example:priya@example.com?secret=$secret&issuer=Example"
        // A QR code of otpauth drawn by the app's own generator; Android has no built-in one.
        val qrCode: Painter? by remember { mutableStateOf(null) }
        val uriHandler = LocalUriHandler.current
        var step by rememberSaveable { mutableIntStateOf(0) }
        var code by rememberSaveable { mutableStateOf("") }
        var codeError by remember { mutableStateOf<String?>(null) }
        var verifying by remember { mutableStateOf(false) }
        val steps = listOf(
            MonarchStepperStep("connect", "Connect app", complete = step > 0),
            MonarchStepperStep("verify", "Enter code", complete = step > 1),
            MonarchStepperStep("save", "Save codes"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            MonarchStepper(steps = steps, current = step)
            when (step) {
                0 -> {
                    MonarchText(text = "Connect an authenticator app", style = MonarchTextStyle.Heading, heading = true)
                    MonarchButton(
                        text = "Open in authenticator app",
                        onClick = {
                            try {
                                uriHandler.openUri(otpauth)
                            } catch (_: IllegalArgumentException) {
                                // No authenticator app is installed: the person copies the key instead.
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                    MonarchCopyField(value = secret, label = "Setup key", helperText = "Or enter this key in the app.")
                    qrCode?.let { qr ->
                        MonarchText(text = "Using another device? Scan this code.", style = MonarchTextStyle.Caption)
                        Image(
                            painter = qr,
                            contentDescription = "QR code for the authenticator app",
                            modifier = Modifier.size(MonarchTheme.spacing.space24 * 2),
                        )
                    }
                    MonarchButton(text = "Next", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    MonarchText(text = "Enter the code from the app", style = MonarchTextStyle.Heading, heading = true)
                    MonarchOtpPinInput(
                        value = code,
                        onValueChange = {
                            code = it
                            codeError = null
                        },
                        label = "6-digit code",
                        errorText = codeError,
                    )
                    MonarchButton(
                        text = "Verify",
                        onClick = {
                            if (code.length < 6) {
                                codeError = "Enter all 6 digits"
                            } else {
                                verifying = true
                                // Check the code on the server, then:
                                verifying = false
                                step = 2
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                        loading = verifying,
                    )
                }
                else -> MonarchRecoveryCodes(
                    codes = listOf("4f7k-2m9q", "8d3r-6t1w", "7n4h-3j8k", "2p6s-5l0z").map { MonarchRecoveryCode(it) },
                    onAcknowledge = { /* Two-step verification is on. */ },
                )
            }
        }
    }
}
