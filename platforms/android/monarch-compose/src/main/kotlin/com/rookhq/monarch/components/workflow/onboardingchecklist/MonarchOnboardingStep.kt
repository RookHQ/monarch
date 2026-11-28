package com.rookhq.monarch.components.workflow.onboardingchecklist

import androidx.compose.runtime.Immutable

/**
 * One step of a `MonarchOnboardingChecklist`, such as "Invite your team".
 *
 * The app decides when a step is done, usually from its own data.
 *
 * @property id a stable id, unique in the checklist. It identifies the row.
 * @property title the step, as a short instruction.
 * @property description one line on why or how. Shown while the step is not done.
 * @property done whether the step is complete.
 */
@Immutable
public class MonarchOnboardingStep(
    public val id: String,
    public val title: String,
    public val description: String? = null,
    public val done: Boolean = false,
)
