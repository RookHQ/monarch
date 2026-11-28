package dev.monarchui.components.workflow.timesheetdiff

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTimesheetDiff`. Every
 * `null` keeps the built-in string `monarch_timesheetdiff_<key>`.
 *
 * @property title the heading, "What changed".
 * @property added the kind of an added entry, "Added".
 * @property removed the kind of a removed entry, "Removed".
 * @property changed the kind of a changed entry, given the changed field names: "Changed time, project".
 * @property fields names of fields in [changed], keyed by [MonarchTimesheetDiffFields] or the product's own
 * names. A field without a name here uses the built-in name, or its raw name when it has none.
 * @property before said before the old line of a changed entry, "Before".
 * @property after said before the new line of a changed entry, "After".
 * @property noProject shown for an entry without a project, "No project".
 * @property nonBillable shown for a non-billable entry, "non-billable".
 * @property noChanges shown when nothing changed, "No changes.".
 * @property delta formats a signed number of minutes for the eye: "+1h 30m", "−45m". TalkBack reads the
 * built-in spoken form ("plus 1 hour, 30 minutes") unless [entry] or this is replaced.
 * @property totals the totals line, given the formatted total and billable changes (`null` when billable totals
 * are off): "Total +1h 30m · billable +45m".
 * @property entry the line of an entry, replacing "Mon, Jan 12 · 2h · Pulse · Design · non-billable". Also what
 * TalkBack reads for the entry. The web's renderEntry, as text.
 */
@Immutable
public class MonarchTimesheetDiffLabels(
    public val title: String? = null,
    public val added: String? = null,
    public val removed: String? = null,
    public val changed: ((List<String>) -> String)? = null,
    public val fields: Map<String, String> = emptyMap(),
    public val before: String? = null,
    public val after: String? = null,
    public val noProject: String? = null,
    public val nonBillable: String? = null,
    public val noChanges: String? = null,
    public val delta: ((Int) -> String)? = null,
    public val totals: ((String, String?) -> String)? = null,
    public val entry: ((MonarchTimesheetDiffEntry) -> String)? = null,
)
