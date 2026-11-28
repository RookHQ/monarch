package dev.monarchui.components.workflow.stepper

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchStepper`. Every `null` uses the built-in string named
 * after it in `strings_stepper.xml`.
 *
 * @property progress the TalkBack name of the stepper: "Progress".
 * @property stepOf writes the position of a step from its number, counted from 1, and the number of steps:
 * "Step 2 of 5".
 * @property complete the state of a finished step: "completed".
 * @property error the state of a step with an error: "needs attention".
 * @property current the state of the current step: "current step".
 * @property optional the note under an optional step: "Optional".
 * @property busy the state added to the current step while the stepper is busy: "in progress".
 */
@Immutable
public class MonarchStepperLabels(
    public val progress: String? = null,
    public val stepOf: ((step: Int, total: Int) -> String)? = null,
    public val complete: String? = null,
    public val error: String? = null,
    public val current: String? = null,
    public val optional: String? = null,
    public val busy: String? = null,
)
