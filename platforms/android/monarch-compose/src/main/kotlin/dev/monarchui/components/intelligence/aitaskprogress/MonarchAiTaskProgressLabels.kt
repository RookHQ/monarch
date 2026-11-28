package dev.monarchui.components.intelligence.aitaskprogress

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiTaskProgress`. Every
 * `null` uses the built-in string in `strings_aitaskprogress.xml`.
 *
 * @property status the name of a status: "Running", "Paused", "Done", "Failed", "Cancelled".
 * @property progressLabel writes the spoken name of the bar from the title: "Summarising 40 documents progress".
 * @property progressValue writes the percentage from a whole number: "40% complete".
 * @property started writes the start text from the relative time: "Started 5 minutes ago".
 * @property eta writes the time left from milliseconds: "About 3 minutes left".
 * @property cancel the cancel button: "Cancel".
 * @property minimize the minimize button, and its spoken name when it shows an icon: "Minimize".
 * @property notify the notify toggle: "Notify me when done".
 * @property announce writes the announcement of a status change from the title and the status name:
 * "Summarising 40 documents: Done".
 */
@Immutable
public class MonarchAiTaskProgressLabels(
    public val status: ((MonarchAiTaskStatus) -> String)? = null,
    public val progressLabel: ((title: String) -> String)? = null,
    public val progressValue: ((percent: Int) -> String)? = null,
    public val started: ((relative: String) -> String)? = null,
    public val eta: ((millis: Long) -> String)? = null,
    public val cancel: String? = null,
    public val minimize: String? = null,
    public val notify: String? = null,
    public val announce: ((title: String, status: String) -> String)? = null,
)
