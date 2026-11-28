package dev.monarchui.components.intelligence.aionboardingtour

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiOnboardingTour`.
 * Every `null` keeps the built-in string `monarch_aionboardingtour_<key>`.
 *
 * @property eyebrow the small label above the stepper, "AI".
 * @property stepOf the step counter and the start of the step announcement, from the step (from 1) and the
 * total: "Step 2 of 4".
 * @property back the button that goes to the previous step, "Back".
 * @property next the button that goes to the next step, "Next".
 * @property finish the button of the last step, "Got it".
 * @property skip the button that skips the tour, "Skip introduction".
 * @property disable the opt-out on every step, "Turn off AI".
 */
@Immutable
public class MonarchAiOnboardingTourLabels(
    public val eyebrow: String? = null,
    public val stepOf: ((step: Int, total: Int) -> String)? = null,
    public val back: String? = null,
    public val next: String? = null,
    public val finish: String? = null,
    public val skip: String? = null,
    public val disable: String? = null,
)
