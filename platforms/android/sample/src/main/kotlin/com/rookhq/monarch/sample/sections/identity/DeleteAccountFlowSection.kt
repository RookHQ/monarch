@file:OptIn(ExperimentalMonarchApi::class, ExperimentalMaterial3Api::class)

package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequest
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportStatus
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpAuthDialog
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The catalogue page of DeleteAccountFlow: owned workspaces, export, reason, confirm, step-up and done. */
object DeleteAccountFlowSection : CataloguePage {
    override val title = "DeleteAccountFlow"

    private const val Email = "sam@example.com"
    private const val SimulatedMillis = 800L
    private const val AcmeMembers = 12

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableStateOf("review") }
        var owned by remember { mutableStateOf(listOf("Acme" to AcmeMembers)) }
        var export by remember { mutableStateOf(MonarchDataExportStatus.Idle) }
        var reason by rememberSaveable { mutableStateOf<String?>(null) }
        var details by rememberSaveable { mutableStateOf("") }
        var typed by rememberSaveable { mutableStateOf("") }
        var deleting by remember { mutableStateOf(false) }
        CatalogueCaption("Delete my account; resolve Acme first")
        MonarchButton(
            text = "Delete account",
            onClick = {
                step = "review"
                typed = ""
                open = true
            },
            variant = MonarchButtonVariant.Danger,
        )
        if (!open) return
        MonarchBottomSheet(onDismissRequest = { open = false }, dismissible = !deleting) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                when (step) {
                    "review" -> Review(owned, onResolve = { name -> owned = owned.filterNot { it.first == name } }) {
                        step = "export"
                    }
                    "export" -> {
                        MonarchDataExportRequest(
                            status = export,
                            onRequest = {
                                delay(SimulatedMillis)
                                export = MonarchDataExportStatus.Requested
                            },
                        )
                        Continue { step = "reason" }
                    }
                    "reason" -> {
                        MonarchText(text = "Why are you leaving?", style = MonarchTextStyle.Title)
                        MonarchText(text = "Optional. Your answer helps us improve.")
                        MonarchSelect(
                            options = listOf(
                                MonarchSelectOption("price", "It costs too much"),
                                MonarchSelectOption("features", "It’s missing something I need"),
                                MonarchSelectOption("other", "Something else"),
                            ),
                            selected = reason,
                            onSelect = { reason = it },
                            label = "Reason",
                            placeholder = "Choose a reason",
                        )
                        MonarchTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = "Anything else you want to tell us?",
                            singleLine = false,
                        )
                        Continue { step = "confirm" }
                    }
                    "confirm" -> {
                        MonarchText(text = "Confirm deletion", style = MonarchTextStyle.Title)
                        MonarchTypeToConfirmInput(
                            phrase = Email,
                            value = typed,
                            onValueChange = { typed = it },
                            label = "Type $Email to confirm",
                        )
                        MonarchButton(
                            text = "Delete account",
                            onClick = { step = "verify" },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Danger,
                            enabled = MonarchTypeToConfirmInputDefaults.matches(typed, Email),
                        )
                    }
                    "verify" -> MonarchStepUpAuthDialog(
                        onVerify = { delay(SimulatedMillis) },
                        description = "Deleting your account needs you to confirm your identity.",
                        onCancel = { step = "confirm" },
                        onVerified = {
                            deleting = true
                            scope.launch {
                                delay(SimulatedMillis)
                                deleting = false
                                step = "done"
                            }
                        },
                        onVerifyingChange = { deleting = it },
                    )
                    else -> {
                        MonarchText(text = "Account deleted", style = MonarchTextStyle.Title)
                        MonarchText(text = "Your account has been removed. Thank you for the time you spent with us.")
                        MonarchButton(
                            text = "Done",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Primary,
                        )
                    }
                }
                if (step != "done" && step != "verify") {
                    MonarchButton(
                        text = "Cancel",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                    )
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.Review(
        owned: List<Pair<String, Int>>,
        onResolve: (String) -> Unit,
        onContinue: () -> Unit,
    ) {
        MonarchText(text = "Before you delete your account", style = MonarchTextStyle.Title)
        MonarchText(text = "Your account and your access to every workspace will be removed. This can’t be undone.")
        MonarchConsequenceList(
            items = listOf(
                MonarchConsequence(MonarchConsequenceKind.Removed, "Your profile and settings"),
                MonarchConsequence(MonarchConsequenceKind.Removed, "Your access to workspaces", count = 3),
                MonarchConsequence(MonarchConsequenceKind.Kept, "Messages you sent stay in their workspaces"),
            ),
            heading = "What happens",
        )
        if (owned.isNotEmpty()) {
            MonarchInlineNotice(
                message = "You are the only owner of these workspaces. Transfer each one, or delete it, first.",
                tone = MonarchTone.Warning,
            )
            owned.forEach { (name, members) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = "$name, $members members", modifier = Modifier.weight(1f))
                    MonarchButton(
                        text = "Transfer or delete",
                        onClick = { onResolve(name) },
                        modifier = Modifier.semantics { contentDescription = "Transfer or delete $name" },
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
        MonarchButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
            enabled = owned.isEmpty(),
        )
    }

    @Composable
    private fun Continue(onClick: () -> Unit) {
        MonarchButton(
            text = "Continue",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
        )
    }
}
