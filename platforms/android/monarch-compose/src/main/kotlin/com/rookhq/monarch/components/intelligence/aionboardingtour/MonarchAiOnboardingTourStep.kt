package com.rookhq.monarch.components.intelligence.aionboardingtour

import androidx.compose.runtime.Immutable

/**
 * One step of a `MonarchAiOnboardingTour`.
 *
 * @property title a short heading, for example "What the assistant can see". Already localised.
 * @property body one or two sentences. Already localised.
 * @property id a stable key. `null` uses the position of the step.
 */
@Immutable
public data class MonarchAiOnboardingTourStep(
    public val title: String,
    public val body: String,
    public val id: String? = null,
)
