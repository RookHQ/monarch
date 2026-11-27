package com.rookhq.monarch.components.intelligence.aichainofsteps

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState

/** The pure logic of MonarchAiChainOfSteps, apart from its colours and sizes. */
internal object MonarchAiChainOfStepsLogic {
    /** Whether a state is final: done, failed or skipped. */
    fun isFinished(state: MonarchAiPlanStepState): Boolean =
        state == MonarchAiPlanStepState.Done ||
            state == MonarchAiPlanStepState.Failed ||
            state == MonarchAiPlanStepState.Skipped

    /** Whether every step has finished. An empty list has not. */
    fun allFinished(steps: List<MonarchAiPlanStep>): Boolean = steps.isNotEmpty() && steps.all { isFinished(it.state) }

    /** The number of failed steps. */
    fun failedCount(steps: List<MonarchAiPlanStep>): Int = steps.count { it.state == MonarchAiPlanStepState.Failed }

    /** Whether the list is folded under a summary: when it may fold and every step has finished. */
    fun folded(steps: List<MonarchAiPlanStep>, collapsible: Boolean): Boolean = collapsible && allFinished(steps)

    /** What TalkBack reads after the label of a step: its state, then its detail. */
    fun spokenState(stateName: String, detail: String?): String =
        if (detail.isNullOrBlank()) stateName else "$stateName, $detail"

    /** The state of every step by id, to compare with the next update. */
    fun snapshot(steps: List<MonarchAiPlanStep>): Map<String, MonarchAiPlanStepState> =
        steps.associate { it.id to it.state }

    /**
     * The announcements for a change from [previous] (state by step id) to
     * [steps]. Nothing is announced for the first state, which is history,
     * not news: a step that starts or fails, then completion once.
     */
    fun announcements(
        previous: Map<String, MonarchAiPlanStepState>?,
        steps: List<MonarchAiPlanStep>,
        texts: MonarchAiChainOfStepsAnnouncementTexts,
    ): List<String> {
        if (previous == null) return emptyList()
        val total = steps.size
        val lines = steps.mapIndexedNotNull { index, step ->
            when {
                previous[step.id] == step.state -> null
                step.state == MonarchAiPlanStepState.Active -> texts.started(step.label, index + 1, total)
                step.state == MonarchAiPlanStepState.Failed -> texts.failedStep(step.label)
                else -> null
            }
        }.toMutableList()
        val wasFinished = previous.size == total && previous.isNotEmpty() && previous.values.all(::isFinished)
        if (allFinished(steps) && !wasFinished) lines += texts.finished(total, failedCount(steps))
        return lines
    }
}
