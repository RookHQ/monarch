package dev.monarchui.components.intelligence.aiagentruntimeline

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiAgentRunTimeline`, and
 * its duration formats. Every `null` keeps the built-in string
 * `monarch_aiagentruntimeline_<key>`.
 *
 * @property label the heading and the name of the trace, "Agent run".
 * @property modeLabel the name of the view switch, "View".
 * @property mode the name of a view: "Timeline", "Table".
 * @property kind the name of an event kind: "Model", "Tool", "Hand-off", "Error".
 * @property events the name of the list of events, "Run events".
 * @property totalDuration the total time, "Total time".
 * @property inputTokens "Input tokens".
 * @property outputTokens "Output tokens".
 * @property cost "Cost".
 * @property start the start offset of an event, "Start".
 * @property duration the duration of an event, "Duration".
 * @property eventColumn the table's event column, "Event".
 * @property kindColumn the table's kind column, "Kind".
 * @property running shown instead of the duration of a running event, "Running".
 * @property empty shown without events, "No events yet".
 * @property notAvailable shown in the table for an unknown value, "–".
 * @property openPart the button that shows the event's part in the conversation, "Show in conversation".
 * @property formatDuration writes a duration in milliseconds, such as "1.6 s".
 * @property formatOffset writes an offset from the start of the run in milliseconds, such as "+1.6 s".
 */
@Immutable
public class MonarchAiAgentRunTimelineLabels(
    public val label: String? = null,
    public val modeLabel: String? = null,
    public val mode: ((MonarchAiAgentRunTimelineMode) -> String)? = null,
    public val kind: ((MonarchAiRunEventKind) -> String)? = null,
    public val events: String? = null,
    public val totalDuration: String? = null,
    public val inputTokens: String? = null,
    public val outputTokens: String? = null,
    public val cost: String? = null,
    public val start: String? = null,
    public val duration: String? = null,
    public val eventColumn: String? = null,
    public val kindColumn: String? = null,
    public val running: String? = null,
    public val empty: String? = null,
    public val notAvailable: String? = null,
    public val openPart: String? = null,
    public val formatDuration: ((Long) -> String)? = null,
    public val formatOffset: ((Long) -> String)? = null,
)
