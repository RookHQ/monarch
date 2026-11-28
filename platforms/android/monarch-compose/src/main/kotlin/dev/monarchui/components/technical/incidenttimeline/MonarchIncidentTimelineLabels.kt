package dev.monarchui.components.technical.incidenttimeline

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchIncidentTimeline`. Every `null` uses the built-in string named
 * after it in `strings_incidenttimeline.xml`.
 *
 * @property list the spoken name of the updates: "Incident updates".
 * @property automated the tag on system-posted updates: "Automated".
 * @property empty shown when there are no updates: "No updates posted yet".
 * @property investigating the label of the investigating stage: "Investigating".
 * @property identified the label of the identified stage: "Identified".
 * @property monitoring the label of the monitoring stage: "Monitoring".
 * @property resolved the label of the resolved stage: "Resolved".
 */
@Immutable
public class MonarchIncidentTimelineLabels(
    public val list: String? = null,
    public val automated: String? = null,
    public val empty: String? = null,
    public val investigating: String? = null,
    public val identified: String? = null,
    public val monitoring: String? = null,
    public val resolved: String? = null,
)
