package com.rookhq.monarch.components.technical.incidenttimeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The story of one incident: each update with its stage (investigating, identified, monitoring, resolved), time and
 * text, joined by a line, with system-posted updates tagged Automated.
 *
 * Built by Monarch from Compose layouts: a `Column` of updates, each a dot on a line, a `MonarchStatusBadge` with the
 * stage (a word and a tone), the time in one zone (UTC by default, with the zone name, so every reader quotes the same
 * time), the author, a `MonarchTag` for automated updates and the text. Updates are shown in the order given; status
 * pages usually show the oldest first. Product stages are added by key with [stages], which also overrides the labels
 * and tones of the standard ones.
 *
 * For TalkBack, the updates are one group named "Incident updates" and each update is one element read as the stage,
 * the time, the author and "Automated", then the text. With [live], an update added while the timeline is shown is
 * announced through a polite live region.
 *
 * @param updates the updates in the order to show them, usually oldest first.
 * @param modifier the modifier of the timeline.
 * @param stages product stages by key, or overrides of the standard ones. Looked up before the standard stages.
 * @param density regular, or compact for cards and side panels.
 * @param live announces an update added while the timeline is shown, for a page that refreshes while someone reads.
 * @param timeZone the zone of the times, as an IANA id such as `"Europe/Berlin"`. UTC by default. `null`, or an
 * id the device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchIncidentTimeline(
    updates: List<MonarchIncidentUpdate>,
    modifier: Modifier = Modifier,
    stages: Map<String, MonarchStatusDefinition> = emptyMap(),
    density: MonarchIncidentTimelineDensity = MonarchIncidentTimelineDensity.Regular,
    live: Boolean = false,
    timeZone: String? = "UTC",
    labels: MonarchIncidentTimelineLabels = MonarchIncidentTimelineLabels(),
) {
    val colors = MonarchTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val zone = monarchTimeZone(timeZone)
    val automated = monarchString(labels.automated, R.string.monarch_incidenttimeline_automated)
    val investigating = monarchString(labels.investigating, R.string.monarch_incidenttimeline_investigating)
    val identified = monarchString(labels.identified, R.string.monarch_incidenttimeline_identified)
    val monitoring = monarchString(labels.monitoring, R.string.monarch_incidenttimeline_monitoring)
    val resolved = monarchString(labels.resolved, R.string.monarch_incidenttimeline_resolved)
    val listName = monarchString(labels.list, R.string.monarch_incidenttimeline_list)
    val standardLabel: (MonarchIncidentStage) -> String = {
        when (it) {
            MonarchIncidentStage.Investigating -> investigating
            MonarchIncidentStage.Identified -> identified
            MonarchIncidentStage.Monitoring -> monitoring
            MonarchIncidentStage.Resolved -> resolved
        }
    }
    val rows = updates.map { update ->
        val definition = MonarchIncidentTimelineRules.resolve(update.stage, stages, standardLabel)
        val time = MonarchIncidentTimelineRules.time(update.time, zone, locale)
        val tag = automated.takeIf { update.automated }
        val spoken = MonarchIncidentTimelineRules.spoken(definition.label, time, update.author, tag, update.body)
        MonarchIncidentTimelineRow(update, definition, time, tag, spoken)
    }
    var announcedCount by remember { mutableIntStateOf(updates.size) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(updates.size) {
        val previous = announcedCount
        announcedCount = updates.size
        if (!live) return@LaunchedEffect
        val added = MonarchIncidentTimelineRules.newUpdate(updates, previous) ?: return@LaunchedEffect
        announcement = rows.first { it.update === added }.spoken
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (updates.isEmpty()) {
            MonarchText(
                text = monarchString(labels.empty, R.string.monarch_incidenttimeline_empty),
                color = MonarchIncidentTimelineTokens.muted(colors),
            )
        } else {
            val column = MonarchIncidentTimelineTokens.markerColumn(LocalDensity.current.fontScale)
            Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = listName }) {
                rows.forEachIndexed { index, row ->
                    MonarchIncidentTimelineUpdate(row, density, column, last = index == rows.lastIndex)
                }
            }
        }
        // Always present, so a new update is announced without moving focus.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}
