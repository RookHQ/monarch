package dev.monarchui.components.workflow.roadmap

import android.content.res.Resources
import dev.monarchui.R
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineGroup
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineItem
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineLabels

/** The roadmap's own strings, resolved once with the app's replacements. */
internal class MonarchRoadmapText(
    val health: (MonarchRoadmapHealth) -> String,
    val count: (Int) -> String,
    val meta: (String, String) -> String,
) {
    companion object {
        /** Resolves [labels] against the built-in strings in [resources]. */
        fun of(labels: MonarchRoadmapLabels, resources: Resources): MonarchRoadmapText = MonarchRoadmapText(
            health = labels.health ?: { resources.getString(MonarchRoadmapRules.healthRes(it)) },
            count = labels.count ?: { resources.getQuantityString(R.plurals.monarch_roadmap_count, it, it) },
            meta = labels.meta ?: { first, second ->
                resources.getString(R.string.monarch_roadmap_meta, first, second)
            },
        )
    }
}

/** The roadmap as a Gantt timeline: rows, group headings and the timeline's strings. */
internal class MonarchRoadmapRows(
    val items: List<MonarchGanttTimelineItem>,
    val groups: List<MonarchGanttTimelineGroup>,
    val byId: Map<String, MonarchRoadmapItem>,
)

/** Pure mapping from roadmap projects and initiatives to Gantt timeline rows. */
internal object MonarchRoadmapRules {
    /** The built-in string of [health]. */
    fun healthRes(health: MonarchRoadmapHealth): Int = when (health) {
        MonarchRoadmapHealth.OnTrack -> R.string.monarch_roadmap_on_track
        MonarchRoadmapHealth.AtRisk -> R.string.monarch_roadmap_at_risk
        MonarchRoadmapHealth.OffTrack -> R.string.monarch_roadmap_off_track
        MonarchRoadmapHealth.NotReported -> R.string.monarch_roadmap_no_health
    }

    /**
     * The text next to a project's name: the progress text. The health is a chip beside it (see
     * [MonarchRoadmapHealthChip]), as on the web, and is read with the row.
     */
    @Suppress("UNUSED_PARAMETER")
    fun itemMeta(item: MonarchRoadmapItem, text: MonarchRoadmapText): String? = item.progressText

    /** What TalkBack reads for a group's chip: the number of projects, then the health in words when given. */
    fun groupDescription(group: MonarchRoadmapGroup, count: Int, text: MonarchRoadmapText): String {
        val counted = text.count(count)
        return group.health?.let { text.meta(counted, text.health(it)) } ?: counted
    }

    /** Maps [items] and [groups] to the timeline's rows. Every project keeps its own dates and is editable. */
    fun rows(
        items: List<MonarchRoadmapItem>,
        groups: List<MonarchRoadmapGroup>,
        text: MonarchRoadmapText,
    ): MonarchRoadmapRows {
        return MonarchRoadmapRows(
            items = items.map { item ->
                MonarchGanttTimelineItem(
                    id = item.id,
                    label = item.label,
                    start = item.start,
                    end = item.end,
                    groupId = item.groupId,
                    progress = item.progress,
                    tone = MonarchRoadmapTokens.barTone(item.tone, item.health),
                    meta = itemMeta(item, text),
                )
            },
            groups = groups.map { group ->
                MonarchGanttTimelineGroup(
                    id = group.id,
                    label = group.label,
                    start = group.start,
                    end = group.end,
                )
            },
            byId = items.associateBy { it.id },
        )
    }

    /** [labels] with the roadmap's chart name and row header where the app gave none. */
    @Suppress("LongMethod")
    fun timelineLabels(labels: MonarchGanttTimelineLabels, timeline: String, rowHeader: String) =
        MonarchGanttTimelineLabels(
            timeline = labels.timeline ?: timeline,
            rowHeader = labels.rowHeader ?: rowHeader,
            zoom = labels.zoom,
            zoomName = labels.zoomName,
            earlier = labels.earlier,
            later = labels.later,
            today = labels.today,
            view = labels.view,
            chart = labels.chart,
            list = labels.list,
            milestones = labels.milestones,
            hint = labels.hint,
            bar = labels.bar,
            conflict = labels.conflict,
            milestone = labels.milestone,
            ended = labels.ended,
            starts = labels.starts,
            noDates = labels.noDates,
            unscheduled = labels.unscheduled,
            changed = labels.changed,
            changeDates = labels.changeDates,
            save = labels.save,
            cancel = labels.cancel,
            duration = labels.duration,
            progress = labels.progress,
            dependsOn = labels.dependsOn,
            expanded = labels.expanded,
            collapsed = labels.collapsed,
            children = labels.children,
            moveEarlier = labels.moveEarlier,
            moveLater = labels.moveLater,
            startEarlier = labels.startEarlier,
            startLater = labels.startLater,
            endEarlier = labels.endEarlier,
            endLater = labels.endLater,
            range = labels.range,
        )
}
