@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.consequencelist.MonarchConsequence
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceKind
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceList
import dev.monarchui.components.forms.entitypicker.MonarchEntityPicker
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.stepper.MonarchStepper
import dev.monarchui.components.workflow.stepper.MonarchStepperStep
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of TransferOwnershipFlow: a recipe of existing components. */
object TransferOwnershipFlowSection : CataloguePage {
    override val title = "TransferOwnershipFlow"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableIntStateOf(0) }
        var newOwner by rememberSaveable { mutableStateOf<String?>(null) }
        var transferredTo by rememberSaveable { mutableStateOf<String?>(null) }
        val members = listOf(
            MonarchEntityPickerEntity("ben", "Ben Okafor", description = "Admin"),
            MonarchEntityPickerEntity("ada", "Ada Moss", description = "Admin"),
            MonarchEntityPickerEntity(
                "chloe",
                "Chloé Martin",
                description = "Member",
                hint = "Not an admin",
                enabled = false,
            ),
        )
        val steps = listOf(
            MonarchStepperStep("pick", "Choose the new owner"),
            MonarchStepperStep("review", "What changes"),
            MonarchStepperStep("confirm", "Confirm"),
        )
        val name = members.firstOrNull { it.id == newOwner }?.name.orEmpty()
        CatalogueCaption("Three steps in a bottom sheet: pick the new owner, review, confirm")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            transferredTo?.let { MonarchBanner(message = "$it is now the owner.", tone = MonarchTone.Success) }
            MonarchButton(
                text = "Transfer ownership",
                onClick = {
                    step = 0
                    newOwner = null
                    open = true
                },
                variant = MonarchButtonVariant.Danger,
            )
        }
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, title = "Transfer ownership") {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchStepper(steps = steps, current = step)
                    when (step) {
                        0 -> MonarchEntityPicker(
                            label = "New owner",
                            entities = members,
                            selected = newOwner,
                            onSelectedChange = { newOwner = it },
                            helperText = "The new owner gets full control of the workspace, including billing.",
                        )
                        1 -> MonarchConsequenceList(
                            items = listOf(
                                MonarchConsequence(MonarchConsequenceKind.Changed, "Your role changes to Admin"),
                                MonarchConsequence(
                                    MonarchConsequenceKind.Removed,
                                    "You can no longer delete the workspace or transfer ownership",
                                ),
                                MonarchConsequence(MonarchConsequenceKind.Changed, "$name becomes the owner"),
                                MonarchConsequence(
                                    MonarchConsequenceKind.Warning,
                                    "$name can change your role or remove you",
                                ),
                            ),
                            heading = "What changes",
                        )
                        else -> MonarchText(text = "Ownership moves to $name. Only the new owner can transfer it back.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                        MonarchButton(
                            text = if (step == 0) "Cancel" else "Back",
                            onClick = { if (step == 0) this@MonarchBottomSheet.dismiss() else step-- },
                            variant = MonarchButtonVariant.Ghost,
                        )
                        MonarchButton(
                            text = if (step == steps.lastIndex) "Transfer ownership" else "Continue",
                            onClick = {
                                if (step == steps.lastIndex) {
                                    // Run the transfer through your API here, then close.
                                    transferredTo = name
                                    this@MonarchBottomSheet.dismiss()
                                } else {
                                    step++
                                }
                            },
                            variant = if (step == steps.lastIndex) {
                                MonarchButtonVariant.Danger
                            } else {
                                MonarchButtonVariant.Primary
                            },
                            enabled = newOwner != null,
                        )
                    }
                }
            }
        }
    }
}
