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
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedRequestMillis = 800L
private const val LastOwnerReason = "You are the only owner. Transfer ownership to another member before you leave."

/** The catalogue page of LeaveWorkspaceFlow: a recipe of an alert dialog, a sheet and MonarchConsequenceList. */
object LeaveWorkspaceFlowSection : CataloguePage {
    override val title = "LeaveWorkspaceFlow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var lastOwner by rememberSaveable { mutableStateOf(false) }
        var blocked by remember { mutableStateOf(false) }
        var confirming by rememberSaveable { mutableStateOf(false) }
        var leaving by remember { mutableStateOf(false) }
        var left by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Leave, or learn why not")
        MonarchSwitchRow(checked = lastOwner, onCheckedChange = { lastOwner = it }, label = "I am the only owner")
        if (lastOwner) {
            MonarchBanner(
                message = LastOwnerReason,
                tone = MonarchTone.Warning,
                title = "You can’t leave yet",
                action = {
                    MonarchButton(text = "Transfer ownership", onClick = {}, size = MonarchButtonSize.Sm)
                },
            )
        }
        if (left) MonarchText(text = "You left Acme.", color = MonarchTheme.colors.textMuted)
        MonarchButton(
            text = "Leave workspace",
            onClick = { if (lastOwner) blocked = true else confirming = true },
            variant = MonarchButtonVariant.Danger,
        )
        if (blocked) {
            MonarchDialog(
                onDismissRequest = { blocked = false },
                title = "You can’t leave yet",
                description = LastOwnerReason,
                actions = {
                    MonarchButton(text = "Close", onClick = { blocked = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(
                        text = "Transfer ownership",
                        onClick = { blocked = false },
                        variant = MonarchButtonVariant.Primary,
                    )
                },
            )
        }
        if (confirming) {
            MonarchBottomSheet(
                onDismissRequest = { confirming = false },
                title = "Leave Acme?",
                description = "You are removed from the workspace straight away.",
                dismissible = !leaving,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchConsequenceList(
                        items = listOf(
                            MonarchConsequence(MonarchConsequenceKind.Removed, "You lose access to Projects and Docs"),
                            MonarchConsequence(
                                MonarchConsequenceKind.Kept,
                                "What you created stays in the workspace for the other members",
                            ),
                            MonarchConsequence(MonarchConsequenceKind.Warning, "You need a new invite to come back"),
                        ),
                    )
                    MonarchButton(
                        text = "Leave workspace",
                        onClick = {
                            leaving = true
                            scope.launch {
                                delay(SimulatedRequestMillis)
                                leaving = false
                                left = true
                                this@MonarchBottomSheet.dismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Danger,
                        loading = leaving,
                    )
                    MonarchButton(
                        text = "Cancel",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                        enabled = !leaving,
                    )
                }
            }
        }
    }
}
