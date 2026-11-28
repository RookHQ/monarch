package dev.monarchui.components.workflow.slaindicator

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSlaIndicator`. Every
 * `null` keeps the built-in string `monarch_slaindicator_<key>` or the
 * built-in formatting.
 *
 * @property onTrack the name of the on-track state, "On track".
 * @property atRisk the name of the at-risk state, "At risk".
 * @property breached the name of the breached state, "Breached".
 * @property paused the name of the paused state, "Paused".
 * @property met the name of the met state, "Met".
 * @property notApplicable the name of the not-applicable state, "No SLA".
 * @property notApplicableMark what is shown for a missing target without `showNotApplicable`, "—".
 * @property remaining the text while time is left, from the duration: "2h 15m left".
 * @property overdue the text once the due time has passed, from the duration: "Overdue 40m".
 * @property duration formats a span of milliseconds (always positive) for the visible text. The built-in
 * format is "<1m", "45m", "2h 15m" or "3d 4h" in the user's locale; TalkBack then hears the units in words.
 * @property due what TalkBack hears about the due time, from the formatted date and time: "due Nov 14, 3:00 PM".
 * @property earlierBreach what TalkBack hears when an earlier target of the item was breached.
 * @property changed what is announced when the state changes, from the state name and the target (or `null`):
 * "First response: Breached", or "SLA Breached" without a target.
 */
@Immutable
public class MonarchSlaIndicatorLabels(
    public val onTrack: String? = null,
    public val atRisk: String? = null,
    public val breached: String? = null,
    public val paused: String? = null,
    public val met: String? = null,
    public val notApplicable: String? = null,
    public val notApplicableMark: String? = null,
    public val remaining: ((String) -> String)? = null,
    public val overdue: ((String) -> String)? = null,
    public val duration: ((Long) -> String)? = null,
    public val due: ((String) -> String)? = null,
    public val earlierBreach: String? = null,
    public val changed: ((String, String?) -> String)? = null,
)
