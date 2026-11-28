package dev.monarchui.components.intelligence.aiagentruntimeline

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R

/** Every string of a timeline, resolved once from the labels and the built-in strings. */
internal class MonarchAiAgentRunTimelineTexts(
    labels: MonarchAiAgentRunTimelineLabels,
    private val resources: Resources,
) {
    private fun pick(override: String?, resource: Int): String = override ?: resources.getString(resource)

    val label: String = pick(labels.label, R.string.monarch_aiagentruntimeline_label)
    val modeLabel: String = pick(labels.modeLabel, R.string.monarch_aiagentruntimeline_mode_label)
    val events: String = pick(labels.events, R.string.monarch_aiagentruntimeline_events)
    val totalDuration: String = pick(labels.totalDuration, R.string.monarch_aiagentruntimeline_total_duration)
    val inputTokens: String = pick(labels.inputTokens, R.string.monarch_aiagentruntimeline_input_tokens)
    val outputTokens: String = pick(labels.outputTokens, R.string.monarch_aiagentruntimeline_output_tokens)
    val cost: String = pick(labels.cost, R.string.monarch_aiagentruntimeline_cost)
    val start: String = pick(labels.start, R.string.monarch_aiagentruntimeline_start)
    val duration: String = pick(labels.duration, R.string.monarch_aiagentruntimeline_duration)
    val eventColumn: String = pick(labels.eventColumn, R.string.monarch_aiagentruntimeline_event_column)
    val kindColumn: String = pick(labels.kindColumn, R.string.monarch_aiagentruntimeline_kind_column)
    val running: String = pick(labels.running, R.string.monarch_aiagentruntimeline_running)
    val empty: String = pick(labels.empty, R.string.monarch_aiagentruntimeline_empty)
    val notAvailable: String = pick(labels.notAvailable, R.string.monarch_aiagentruntimeline_not_available)
    val openPart: String = pick(labels.openPart, R.string.monarch_aiagentruntimeline_open_part)
    val expanded: String = resources.getString(R.string.monarch_collapsible_expanded)
    val collapsed: String = resources.getString(R.string.monarch_collapsible_collapsed)

    private val modes = MonarchAiAgentRunTimelineMode.entries.associateWith {
        labels.mode?.invoke(it) ?: resources.getString(
            if (it == MonarchAiAgentRunTimelineMode.Timeline) {
                R.string.monarch_aiagentruntimeline_mode_timeline
            } else {
                R.string.monarch_aiagentruntimeline_mode_table
            },
        )
    }
    private val kinds = MonarchAiRunEventKind.entries.associateWith {
        labels.kind?.invoke(it) ?: resources.getString(kindResource(it))
    }

    /** The name of a view. */
    val mode: (MonarchAiAgentRunTimelineMode) -> String = { modes.getValue(it) }

    /** The name of an event kind. */
    val kind: (MonarchAiRunEventKind) -> String = { kinds.getValue(it) }

    /** A duration in milliseconds. */
    val formatDuration: (Long) -> String = labels.formatDuration
        ?: { MonarchAiAgentRunTimelineFormat.formatDuration(it, resources) }

    /** An offset from the start of the run in milliseconds. */
    val formatOffset: (Long) -> String = labels.formatOffset
        ?: { resources.getString(R.string.monarch_aiagentruntimeline_offset, formatDuration(it)) }

    /** One fact as TalkBack reads it, "Total time, 4.2 s". */
    val fact: (String, String) -> String = { name, value ->
        resources.getString(R.string.monarch_aiagentruntimeline_fact, name, value)
    }
}

/** Resolves [labels] against the built-in strings, again when the resources change. */
@Composable
internal fun rememberMonarchAiAgentRunTimelineTexts(
    labels: MonarchAiAgentRunTimelineLabels,
): MonarchAiAgentRunTimelineTexts {
    val resources = LocalResources.current
    return remember(labels, resources) { MonarchAiAgentRunTimelineTexts(labels, resources) }
}

private fun kindResource(kind: MonarchAiRunEventKind): Int = when (kind) {
    MonarchAiRunEventKind.Model -> R.string.monarch_aiagentruntimeline_kind_model
    MonarchAiRunEventKind.Tool -> R.string.monarch_aiagentruntimeline_kind_tool
    MonarchAiRunEventKind.Handoff -> R.string.monarch_aiagentruntimeline_kind_handoff
    MonarchAiRunEventKind.Error -> R.string.monarch_aiagentruntimeline_kind_error
}
