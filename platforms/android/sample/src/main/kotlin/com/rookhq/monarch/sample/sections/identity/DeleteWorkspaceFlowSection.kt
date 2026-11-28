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
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import com.rookhq.monarch.components.identity.stepupauthdialog.MonarchStepUpAuthDialog
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedDeleteMillis = 800L
private const val Workspace = "Acme"

/** The catalogue page of DeleteWorkspaceFlow: a sheet with the impact, the name check, step-up and done. */
object DeleteWorkspaceFlowSection : CataloguePage {
    override val title = "DeleteWorkspaceFlow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableStateOf("review") }
        var typed by rememberSaveable { mutableStateOf("") }
        var deleting by remember { mutableStateOf(false) }
        var deleted by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Delete a workspace")
        if (deleted) MonarchText(text = "$Workspace was deleted.", color = MonarchTheme.colors.textMuted)
        MonarchButton(
            text = "Delete workspace",
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
                    "review" -> {
                        MonarchText(text = "Delete $Workspace?", style = MonarchTextStyle.Title)
                        MonarchText(text = "Everything in the workspace is deleted at once. This can’t be undone.")
                        MonarchConsequenceList(
                            items = listOf(
                                MonarchConsequence(MonarchConsequenceKind.Removed, "Apps", count = 4),
                                MonarchConsequence(MonarchConsequenceKind.Removed, "Records", count = 12_480),
                                MonarchConsequence(MonarchConsequenceKind.Changed, "Members lose access", count = 9),
                                MonarchConsequence(MonarchConsequenceKind.Warning, "The paid plan is cancelled"),
                            ),
                            heading = "What will be deleted",
                        )
                        MonarchButton(text = "Export data", onClick = {}, modifier = Modifier.fillMaxWidth())
                        MonarchTypeToConfirmInput(
                            phrase = Workspace,
                            value = typed,
                            onValueChange = { typed = it },
                            label = "Type $Workspace to confirm",
                        )
                        MonarchButton(
                            text = "Delete workspace",
                            onClick = { step = "verify" },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Danger,
                            enabled = MonarchTypeToConfirmInputDefaults.matches(typed, Workspace),
                        )
                        MonarchButton(
                            text = "Cancel",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Ghost,
                        )
                    }
                    "verify" -> MonarchStepUpAuthDialog(
                        onVerify = { delay(SimulatedDeleteMillis) },
                        description = "Deleting a workspace needs you to confirm it’s you.",
                        onCancel = { step = "review" },
                        onVerified = {
                            deleting = true
                            scope.launch {
                                delay(SimulatedDeleteMillis)
                                deleting = false
                                deleted = true
                                step = "done"
                            }
                        },
                        onVerifyingChange = { deleting = it },
                    )
                    else -> {
                        MonarchText(text = "$Workspace was deleted", style = MonarchTextStyle.Title)
                        MonarchText(text = "Members were signed out of it and its plan was cancelled.")
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
