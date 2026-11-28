package com.rookhq.monarch.components.technical.domainprogress

import com.rookhq.monarch.foundation.MonarchTone

/** The pure rules of MonarchDomainProgress: step states, problems, the DNS line and the time between checks. */
internal object MonarchDomainProgressRules {
    /** The separator between the parts TalkBack reads. */
    const val Separator: String = ", "

    private const val Arrow = " → "
    private const val TypeSeparator = " · "
    private const val ListSeparator = ", "
    private const val ShortestRecheck = 15_000L
    private const val LongestRecheck = 300_000L
    private const val DefaultRecheck = 30_000L
    private const val UnknownStageRecheck = 15_000L

    /** The steps in order. */
    val Steps: List<MonarchDomainStep> = MonarchDomainStep.entries

    /** The state of [step] at [stage], before problems. */
    fun baseState(step: MonarchDomainStep, stage: MonarchDomainStage): MonarchDomainStepState {
        val dnsDone = stage != MonarchDomainStage.Dns && stage != MonarchDomainStage.Ownership
        val certificateDone = stage == MonarchDomainStage.Page || stage == MonarchDomainStage.Live
        return when (step) {
            MonarchDomainStep.Dns -> if (dnsDone) MonarchDomainStepState.Done else MonarchDomainStepState.Current
            MonarchDomainStep.Certificate -> when {
                certificateDone -> MonarchDomainStepState.Done
                dnsDone -> MonarchDomainStepState.Current
                else -> MonarchDomainStepState.Todo
            }
            MonarchDomainStep.Live -> when (stage) {
                MonarchDomainStage.Live -> MonarchDomainStepState.Done
                MonarchDomainStage.Page -> MonarchDomainStepState.Current
                else -> MonarchDomainStepState.Todo
            }
        }
    }

    /**
     * The state of each step at [stage] with [problems]: a blocking problem marks its step (the current one when it
     * names none) as needing attention, unless the step is done.
     */
    fun states(
        stage: MonarchDomainStage,
        problems: List<MonarchDomainProblem>,
    ): Map<MonarchDomainStep, MonarchDomainStepState> {
        val states = Steps.associateWith { baseState(it, stage) }.toMutableMap()
        val current = Steps.firstOrNull { states[it] == MonarchDomainStepState.Current }
        problems.filter { it.severity == MonarchDomainProblemSeverity.Blocking }.forEach { problem ->
            val step = problem.step ?: current
            if (step != null && states[step] != MonarchDomainStepState.Done) states[step] = MonarchDomainStepState.Error
        }
        return states
    }

    /** The position of the step to announce: the first in progress or needing attention, else the last. */
    fun activeIndex(items: List<MonarchDomainProgressItem>): Int =
        items.indexOfFirst { it.state == MonarchDomainStepState.Current || it.state == MonarchDomainStepState.Error }
            .takeIf { it >= 0 } ?: (items.size - 1).coerceAtLeast(0)

    /** What TalkBack reads as the state of a step: its spoken state, then its detail. */
    fun stepState(state: String, detail: String): String =
        listOf(state, detail).filter { it.isNotEmpty() }.joinToString(Separator)

    /** The blocking problems, or else the first informational one. */
    fun shownProblems(problems: List<MonarchDomainProblem>): List<MonarchDomainProblem> {
        val blocking = problems.filter { it.severity == MonarchDomainProblemSeverity.Blocking }
        if (blocking.isNotEmpty()) return blocking
        return listOfNotNull(problems.firstOrNull { it.severity == MonarchDomainProblemSeverity.Info })
    }

    /** The tone of a shown problem: warning when blocking, neutral otherwise. */
    fun tone(problem: MonarchDomainProblem): MonarchTone =
        if (problem.severity == MonarchDomainProblemSeverity.Blocking) MonarchTone.Warning else MonarchTone.Neutral

    /** A DNS snapshot as one line: the first CNAME, or the A and AAAA addresses, or [empty]. */
    fun snapshotText(snapshot: MonarchDomainDnsSnapshot, empty: String): String {
        val cname = snapshot.cnames.firstOrNull()
        val parts = buildList {
            if (snapshot.a.isNotEmpty()) add("A" + Arrow + snapshot.a.joinToString(ListSeparator))
            if (snapshot.aaaa.isNotEmpty()) add("AAAA" + Arrow + snapshot.aaaa.joinToString(ListSeparator))
        }
        return when {
            cname != null -> "CNAME$Arrow$cname"
            parts.isNotEmpty() -> parts.joinToString(TypeSeparator)
            else -> empty
        }
    }

    /** See [MonarchDomainProgressDefaults.recheckIntervalMillis]. */
    fun recheckIntervalMillis(stage: MonarchDomainStage?, suggestedMillis: Long?, stopped: Boolean): Long? = when {
        stage == null -> UnknownStageRecheck
        stage == MonarchDomainStage.Live || stopped -> null
        else -> (suggestedMillis ?: DefaultRecheck).coerceIn(ShortestRecheck, LongestRecheck)
    }
}
