package com.rookhq.monarch.components.intelligence.aichainofsteps

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState

/**
 * Replacements for the built-in strings of a `MonarchAiChainOfSteps`. Every
 * `null` keeps the built-in string `monarch_aichainofsteps_<key>`.
 *
 * @property label the name TalkBack reads for the list, "Steps".
 * @property state the name of a state: "Not started", "In progress", "Done", "Skipped", "Failed".
 * @property summary the folded summary, from the number of steps and of failed steps: "4 steps, 1 failed".
 * @property started announced when a step starts, from its label, position and the number of steps:
 * "Step 2 of 4: Reading the contract".
 * @property failedStep announced when a step fails, from its label: "Step failed: Reading the contract".
 * @property finished announced once when every step has finished, from the number of steps and of failed
 * steps: "Finished 4 steps".
 */
@Immutable
public class MonarchAiChainOfStepsLabels(
    public val label: String? = null,
    public val state: ((MonarchAiPlanStepState) -> String)? = null,
    public val summary: ((count: Int, failed: Int) -> String)? = null,
    public val started: ((label: String, position: Int, total: Int) -> String)? = null,
    public val failedStep: ((label: String) -> String)? = null,
    public val finished: ((count: Int, failed: Int) -> String)? = null,
)
