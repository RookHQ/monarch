package com.rookhq.monarch.components.workflow.escalationpolicyeditor

import androidx.compose.runtime.Immutable

/**
 * One level of an escalation policy, the value of a `MonarchEscalationPolicyEditor`.
 *
 * @property id a stable, unique id.
 * @property afterMinutes minutes after the incident or request started, or after the previous level with
 * [MonarchEscalationTiming.Interval].
 * @property targets the ids of the targets notified at this level.
 */
@Immutable
public data class MonarchEscalationLevel(
    public val id: String,
    public val afterMinutes: Int,
    public val targets: List<String> = emptyList(),
) {
    public companion object {
        /**
         * What is wrong with each level: no targets, or, with [MonarchEscalationTiming.Cumulative], not later than
         * the level before. Keyed by level id; empty when the policy is valid.
         */
        public fun validate(
            levels: List<MonarchEscalationLevel>,
            timing: MonarchEscalationTiming = MonarchEscalationTiming.Cumulative,
        ): Map<String, MonarchEscalationLevelProblem> = buildMap {
            var previous: MonarchEscalationLevel? = null
            for (level in levels) {
                val before = previous
                when {
                    level.targets.isEmpty() -> put(level.id, MonarchEscalationLevelProblem.Targets)
                    timing == MonarchEscalationTiming.Cumulative && before != null &&
                        level.afterMinutes <= before.afterMinutes -> put(level.id, MonarchEscalationLevelProblem.Order)
                }
                previous = level
            }
        }
    }
}
