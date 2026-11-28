package dev.monarchui.sample.sections.identity

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.identity.stepupauthdialog.MonarchStepUpAuthDialog
import dev.monarchui.components.identity.stepupauthdialog.MonarchStepUpCredential
import dev.monarchui.components.identity.stepupauthdialog.MonarchStepUpMethod
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedCheckMillis = 600L

/** The catalogue page of StepUpAuthDialog. */
object MonarchStepUpAuthDialogSection : CataloguePage {
    override val title = "StepUpAuthDialog"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var verifying by remember { mutableStateOf(false) }
        var result by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("The password is “correct”, the code 123456.")
        MonarchButton(text = "Delete workspace", onClick = { open = true }, variant = MonarchButtonVariant.Danger)
        result?.let { MonarchText(text = it) }
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, dismissible = !verifying) {
                MonarchStepUpAuthDialog(
                    onVerify = { credential ->
                        delay(SimulatedCheckMillis)
                        val accepted = when (credential) {
                            is MonarchStepUpCredential.Password -> credential.password == "correct"
                            is MonarchStepUpCredential.Code -> credential.code == "123456"
                            else -> true
                        }
                        check(accepted) { "That did not match. Try again." }
                    },
                    description = "Deleting a workspace needs you to confirm it’s you.",
                    methods = listOf(
                        MonarchStepUpMethod.Password,
                        MonarchStepUpMethod.Code,
                        MonarchStepUpMethod.Passkey,
                    ),
                    errorMessage = { it.message },
                    onForgot = { result = "Password recovery would start here." },
                    onCancel = { dismiss() },
                    onVerified = {
                        dismiss()
                        result = "Confirmed. The workspace would be deleted now."
                    },
                    onVerifyingChange = { verifying = it },
                )
            }
        }
    }
}
