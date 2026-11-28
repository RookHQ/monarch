package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.workflow.stepper.MonarchStepper
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperCompactMode
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperOrientation
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperStep
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Stepper: a wizard whose reached steps can be selected. */
object MonarchStepperSection : CataloguePage {
    override val title = "Stepper"

    private val steps = listOf(
        MonarchStepperStep(id = "basics", label = "Basics"),
        MonarchStepperStep(id = "access", label = "Access"),
        MonarchStepperStep(id = "branding", label = "Branding", description = "Logo and colors", optional = true),
        MonarchStepperStep(id = "review", label = "Review"),
    )

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableIntStateOf(1) }
        var furthest by rememberSaveable { mutableIntStateOf(1) }
        var vertical by rememberSaveable { mutableStateOf(false) }
        var failed by rememberSaveable { mutableStateOf(false) }
        val go = { index: Int ->
            current = index
            furthest = maxOf(furthest, index)
        }
        val shown = steps.mapIndexed { index, step ->
            if (index == 1 && failed) MonarchStepperStep(step.id, step.label, error = true) else step
        }
        MonarchSwitchRow(checked = vertical, onCheckedChange = { vertical = it }, label = "Vertical")
        MonarchSwitchRow(checked = failed, onCheckedChange = { failed = it }, label = "Access needs attention")
        CatalogueCaption("A wizard")
        MonarchStepper(
            steps = shown,
            current = current,
            furthest = furthest,
            orientation = if (vertical) MonarchStepperOrientation.Vertical else MonarchStepperOrientation.Horizontal,
            onSelect = { _, index -> go(index) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchButton(text = "Back", onClick = { go(current - 1) }, enabled = current > 0)
            MonarchButton(
                text = "Continue",
                onClick = { go(current + 1) },
                variant = MonarchButtonVariant.Primary,
                enabled = current < steps.lastIndex,
            )
        }
        CatalogueCaption("Never compact")
        MonarchStepper(steps = steps.dropLast(1), current = 1, compact = MonarchStepperCompactMode.Never)
        CatalogueCaption("Always compact")
        MonarchStepper(steps = steps, currentId = "branding", compact = MonarchStepperCompactMode.Always)
    }
}
