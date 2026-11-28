package com.rookhq.monarch.components.workflow.stepper

import androidx.compose.runtime.Immutable

/**
 * One step of a `MonarchStepper`.
 *
 * @property id a stable identifier, unique in the stepper. Passed back to `onSelect` with the step and used by
 * the `currentId` overload.
 * @property label the short name of the step, such as "Details". Already localised.
 * @property description secondary text under the label.
 * @property optional the step may be skipped: "Optional" is shown under the label.
 * @property complete marks the step finished out of order, even when it comes after the current step.
 * @property error marks a step that needs attention. It wins over every other state.
 * @property enabled whether the step can be selected when the stepper has `onSelect`. A disabled step is never a
 * button and its label is in the disabled text colour.
 */
@Immutable
public class MonarchStepperStep(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val optional: Boolean = false,
    public val complete: Boolean = false,
    public val error: Boolean = false,
    public val enabled: Boolean = true,
)
