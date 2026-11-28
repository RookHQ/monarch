package dev.monarchui.components.intelligence.aisubagenttree

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.TimeZone

/** Every string of a subagent tree, from the labels and the built-in strings, in one locale and time zone. */
internal class MonarchAiSubagentTreeTexts(
    private val labels: MonarchAiSubagentTreeLabels,
    private val resources: Resources,
    locale: Locale,
    timeZone: TimeZone,
) {
    private val numbers = NumberFormat.getIntegerInstance(locale)
    private val times = DateFormat.getTimeInstance(DateFormat.SHORT, locale).apply { this.timeZone = timeZone }

    private fun pick(override: String?, resource: Int): String = override ?: resources.getString(resource)

    private fun count(value: Int): String = numbers.format(value.toLong())

    val label: String = pick(labels.label, R.string.monarch_aisubagenttree_label)
    val empty: String = pick(labels.empty, R.string.monarch_aisubagenttree_empty)
    val stop: String = pick(labels.stop, R.string.monarch_aisubagenttree_stop)
    val open: String = pick(labels.open, R.string.monarch_aisubagenttree_open)
    val respond: String = pick(labels.respond, R.string.monarch_aisubagenttree_respond)
    val openPart: String = pick(labels.openPart, R.string.monarch_aisubagenttree_open_part)

    /** The name of a status. */
    fun status(status: MonarchAiSubagentStatus): String = labels.status?.invoke(status) ?: resources.getString(
        when (status) {
            MonarchAiSubagentStatus.Working -> R.string.monarch_aisubagenttree_status_working
            MonarchAiSubagentStatus.NeedsInput -> R.string.monarch_aisubagenttree_status_needs_input
            MonarchAiSubagentStatus.Waiting -> R.string.monarch_aisubagenttree_status_waiting
            MonarchAiSubagentStatus.Done -> R.string.monarch_aisubagenttree_status_done
            MonarchAiSubagentStatus.Failed -> R.string.monarch_aisubagenttree_status_failed
            MonarchAiSubagentStatus.Stopped -> R.string.monarch_aisubagenttree_status_stopped
        },
    )

    private fun statusCount(status: MonarchAiSubagentStatus, value: Int): String {
        labels.statusCount?.invoke(status, value)?.let { return it }
        if (status == MonarchAiSubagentStatus.NeedsInput) {
            return resources.getQuantityString(R.plurals.monarch_aisubagenttree_count_needs_input, value, count(value))
        }
        val resource = when (status) {
            MonarchAiSubagentStatus.Working -> R.string.monarch_aisubagenttree_count_working
            MonarchAiSubagentStatus.Waiting -> R.string.monarch_aisubagenttree_count_waiting
            MonarchAiSubagentStatus.Done -> R.string.monarch_aisubagenttree_count_done
            MonarchAiSubagentStatus.Failed -> R.string.monarch_aisubagenttree_count_failed
            else -> R.string.monarch_aisubagenttree_count_stopped
        }
        return resources.getString(resource, count(value))
    }

    /** The summary over the tree, "4 agents: 2 working, 1 needs you, 1 done", or the empty text. */
    fun summary(agents: List<MonarchAiSubagent>): String {
        val counts = MonarchAiSubagentTreeTokens.counts(agents)
        val total = counts.values.sum()
        if (total == 0) return empty
        val agentsText = labels.agentCount?.invoke(total)
            ?: resources.getQuantityString(R.plurals.monarch_aisubagenttree_agent_count, total, count(total))
        val parts = MonarchAiSubagentStatus.entries.mapNotNull { status ->
            counts[status]?.takeIf { it > 0 }?.let { statusCount(status, it) }
        }
        val breakdown = parts.joinToString(resources.getString(R.string.monarch_aisubagenttree_separator))
        return resources.getString(R.string.monarch_aisubagenttree_summary, agentsText, breakdown)
    }

    /** The facts line of an agent: steps, tool calls and times. */
    fun facts(agent: MonarchAiSubagent): List<String> = buildList {
        agent.stepsDone?.let { done ->
            add(
                labels.steps?.invoke(done, agent.stepsTotal) ?: if (agent.stepsTotal == null) {
                    resources.getQuantityString(R.plurals.monarch_aisubagenttree_steps, done, count(done))
                } else {
                    resources.getString(R.string.monarch_aisubagenttree_steps_of, count(done), count(agent.stepsTotal))
                },
            )
        }
        agent.toolCalls?.let {
            add(
                labels.toolCalls?.invoke(it)
                    ?: resources.getQuantityString(R.plurals.monarch_aisubagenttree_tool_calls, it, count(it)),
            )
        }
        agent.startedAt?.let { start ->
            val startText = times.format(start)
            val end = agent.endedAt
            add(
                if (end == null) {
                    labels.started?.invoke(startText)
                        ?: resources.getString(R.string.monarch_aisubagenttree_started, startText)
                } else {
                    val endText = times.format(end)
                    labels.ran?.invoke(startText, endText)
                        ?: resources.getString(R.string.monarch_aisubagenttree_ran, startText, endText)
                },
            )
        }
    }

    /** The spoken name of the selected agent's actions. */
    fun actionsFor(name: String): String =
        labels.actionsFor?.invoke(name) ?: resources.getString(R.string.monarch_aisubagenttree_actions_for, name)

    /** The announcement of a status change. */
    fun announce(agent: MonarchAiSubagent): String {
        val status = status(agent.status)
        return labels.announce?.invoke(agent.name, status)
            ?: resources.getString(R.string.monarch_aisubagenttree_announce, agent.name, status)
    }

    /** The spoken name of an agent's open or close button. */
    fun toggle(name: String, open: Boolean): String = if (open) {
        labels.collapse?.invoke(name) ?: resources.getString(R.string.monarch_aisubagenttree_collapse, name)
    } else {
        labels.expand?.invoke(name) ?: resources.getString(R.string.monarch_aisubagenttree_expand, name)
    }
}

/** Resolves [labels] against the built-in strings, again when the resources, locale or zone change. */
@Composable
internal fun rememberMonarchAiSubagentTreeTexts(
    labels: MonarchAiSubagentTreeLabels,
    locale: Locale,
    timeZone: TimeZone,
): MonarchAiSubagentTreeTexts {
    val resources = LocalResources.current
    return remember(labels, resources, locale, timeZone) {
        MonarchAiSubagentTreeTexts(labels, resources, locale, timeZone)
    }
}
