package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
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

private const val SimulatedEraseMillis = 800L
private const val Subject = "jane.doe@example.com"

/** The catalogue page of GdprEraseFlow: what is erased and kept, the acknowledgement, step-up and the reference. */
object GdprEraseFlowSection : CataloguePage {
    override val title = "GdprEraseFlow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableStateOf("review") }
        var understood by rememberSaveable { mutableStateOf(false) }
        var typed by rememberSaveable { mutableStateOf("") }
        var erasing by remember { mutableStateOf(false) }
        var auditId by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Erase one person's data")
        auditId?.let { MonarchText(text = "Erased. Reference: $it", color = MonarchTheme.colors.textMuted) }
        MonarchButton(
            text = "Erase personal data",
            onClick = {
                step = "review"
                understood = false
                typed = ""
                open = true
            },
            variant = MonarchButtonVariant.Danger,
        )
        if (!open) return
        MonarchBottomSheet(onDismissRequest = { open = false }, dismissible = !erasing) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                when (step) {
                    "review" -> {
                        MonarchText(text = "Erase Jane Doe’s personal data?", style = MonarchTextStyle.Title)
                        MonarchConsequenceList(
                            items = listOf(
                                MonarchConsequence(MonarchConsequenceKind.Removed, "Profile and contact details"),
                                MonarchConsequence(MonarchConsequenceKind.Removed, "Messages", count = 312),
                                MonarchConsequence(MonarchConsequenceKind.Removed, "Uploaded files", count = 18),
                            ),
                            heading = "Will be erased",
                        )
                        MonarchConsequenceList(
                            items = listOf(
                                MonarchConsequence(
                                    MonarchConsequenceKind.Kept,
                                    "Invoices",
                                    detail = "Kept for 7 years to meet tax law",
                                ),
                            ),
                            heading = "Will be kept",
                        )
                        MonarchInlineNotice(
                            message = "Erased data can’t be recovered, also not from backups.",
                            tone = MonarchTone.Warning,
                            title = "This can’t be undone",
                        )
                        MonarchCheckboxRow(
                            checked = understood,
                            onCheckedChange = { understood = it },
                            label = "I understand the data is erased for good",
                        )
                        MonarchTypeToConfirmInput(
                            phrase = Subject,
                            value = typed,
                            onValueChange = { typed = it },
                            label = "Type $Subject to confirm",
                        )
                        MonarchButton(
                            text = "Erase data",
                            onClick = { step = "verify" },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Danger,
                            enabled = understood && MonarchTypeToConfirmInputDefaults.matches(typed, Subject),
                        )
                        MonarchButton(
                            text = "Cancel",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Ghost,
                        )
                    }
                    "verify" -> MonarchStepUpAuthDialog(
                        onVerify = { delay(SimulatedEraseMillis) },
                        description = "Erasing someone’s data needs you to confirm it’s you.",
                        onCancel = { step = "review" },
                        onVerified = {
                            erasing = true
                            scope.launch {
                                delay(SimulatedEraseMillis)
                                auditId = "AUD-20261114-0042"
                                erasing = false
                                step = "done"
                            }
                        },
                        onVerifyingChange = { erasing = it },
                    )
                    else -> {
                        MonarchText(text = "Personal data erased", style = MonarchTextStyle.Title)
                        auditId?.let { MonarchText(text = "Logged in the audit log. Reference: $it") }
                        MonarchButton(
                            text = "Done",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Primary,
                        )
                    }
                }
            }
        }
    }
}
