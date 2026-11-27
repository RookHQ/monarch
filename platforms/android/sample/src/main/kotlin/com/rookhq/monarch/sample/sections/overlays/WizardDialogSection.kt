package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.stepper.MonarchStepper
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperStep
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of WizardDialog, a recipe: MonarchStepper and the step's content in a bottom sheet. */
object WizardDialogSection : CataloguePage {
    override val title = "WizardDialog"

    private val steps = listOf(
        MonarchStepperStep(id = "destination", label = "Destination"),
        MonarchStepperStep(id = "review", label = "Review"),
        MonarchStepperStep(id = "confirm", label = "Confirm"),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableIntStateOf(0) }
        var destination by rememberSaveable { mutableStateOf("") }
        var moved by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Move a project in three steps")
        MonarchButton(
            text = "Move project",
            onClick = {
                step = 0
                open = true
            },
        )
        if (moved.isNotEmpty()) MonarchText(text = moved, style = MonarchTextStyle.Caption)
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchStepper(steps = steps, current = step)
                    MonarchText(
                        text = steps[step].label,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MonarchTextStyle.Heading,
                    )
                    when (step) {
                        0 -> MonarchTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = "Workspace",
                        )
                        1 -> MonarchText(text = "Moving to $destination. Links to the project keep working.")
                        else -> MonarchText(text = "Members of the old workspace lose access to the project.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                        if (step > 0) {
                            MonarchButton(text = "Back", onClick = { step-- }, variant = MonarchButtonVariant.Ghost)
                        }
                        Spacer(Modifier.weight(1f))
                        val last = step == steps.lastIndex
                        MonarchButton(
                            text = if (last) "Move project" else "Continue",
                            onClick = {
                                if (last) {
                                    moved = "Moved to $destination."
                                    this@MonarchBottomSheet.dismiss()
                                } else {
                                    step++
                                }
                            },
                            variant = if (last) MonarchButtonVariant.Danger else MonarchButtonVariant.Primary,
                            enabled = destination.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}
