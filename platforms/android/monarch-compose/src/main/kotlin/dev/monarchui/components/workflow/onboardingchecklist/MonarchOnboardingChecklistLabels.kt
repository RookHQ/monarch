package dev.monarchui.components.workflow.onboardingchecklist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchOnboardingChecklist`.
 * Every `null` uses the built-in string named after it in
 * `strings_onboardingchecklist.xml`; [expanded] and [collapsed] fall back to
 * the strings of `MonarchCollapsible`.
 *
 * @property progress writes the progress from the done and total step counts: "2 of 5 done".
 * @property progressLabel the spoken name of the progress bar: "Setup progress".
 * @property dismiss the label of the dismiss button, or its spoken name when it shows an icon: "Dismiss checklist".
 * @property done the state of a done step: "Done".
 * @property notDone the state of a step that is not done: "Not done".
 * @property current added to the state of the first open step: "Current step".
 * @property complete the message shown when every step is done: "All done. You are set up.".
 * @property expanded the state of the progress row while the steps are shown: "Expanded".
 * @property collapsed the state of the progress row while the steps are hidden: "Collapsed".
 */
@Immutable
public class MonarchOnboardingChecklistLabels(
    public val progress: ((done: Int, total: Int) -> String)? = null,
    public val progressLabel: String? = null,
    public val dismiss: String? = null,
    public val done: String? = null,
    public val notDone: String? = null,
    public val current: String? = null,
    public val complete: String? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
)
