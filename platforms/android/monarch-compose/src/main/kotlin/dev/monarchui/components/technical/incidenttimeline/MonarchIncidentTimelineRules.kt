package dev.monarchui.components.technical.incidenttimeline

import android.icu.text.DateFormat
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.foundation.MonarchTone
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The pure rules of MonarchIncidentTimeline: stage labels and tones, times and what TalkBack reads. */
internal object MonarchIncidentTimelineRules {
    /** The skeleton of the time: month, day, 24-hour time and the zone name ("Nov 14, 09:30 UTC"). */
    const val TimeSkeleton: String = "MMMdHHmmz"

    private const val Separator = ", "
    private const val BodySeparator = ". "

    /** The built-in tone of a standard stage. */
    fun tone(stage: MonarchIncidentStage): MonarchTone = when (stage) {
        MonarchIncidentStage.Investigating -> MonarchTone.Danger
        MonarchIncidentStage.Identified -> MonarchTone.Warning
        MonarchIncidentStage.Monitoring -> MonarchTone.Info
        MonarchIncidentStage.Resolved -> MonarchTone.Success
    }

    /**
     * The label and tone of a stage key: a product stage from [stages] first (matched exactly), then a standard stage
     * (ignoring case) with its label from [standardLabel], else the key made readable with the neutral tone.
     */
    fun resolve(
        stage: String,
        stages: Map<String, MonarchStatusDefinition>,
        standardLabel: (MonarchIncidentStage) -> String,
    ): MonarchStatusDefinition {
        stages[stage]?.let { return it }
        val standard = MonarchIncidentStage.entries.firstOrNull { it.key == stage.lowercase(Locale.ROOT) }
        return if (standard != null) {
            MonarchStatusDefinition(standardLabel(standard), tone(standard))
        } else {
            MonarchStatusDefinition(readable(stage), MonarchTone.Neutral)
        }
    }

    /**
     * A key made readable: underscores and hyphens become spaces, the first letter is upper case and the rest lower
     * case ("partial_outage" is "Partial outage"). A key with no letters stays as it is.
     */
    fun readable(key: String): String {
        val words = key.replace('_', ' ').replace('-', ' ').split(' ').filter { it.isNotEmpty() }
            .joinToString(" ").lowercase(Locale.ROOT)
        if (words.isEmpty()) return key
        return words.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

    /** The time of an update in a fixed zone, with the zone name, so readers quote the same time. */
    fun time(time: Long, zone: TimeZone, locale: Locale): String {
        val format = DateFormat.getInstanceForSkeleton(TimeSkeleton, locale)
        format.timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
        return format.format(Date(time))
    }

    /** What TalkBack reads for an update: the stage, the time, the author and Automated, then the body. */
    fun spoken(stage: String, time: String, author: String?, automated: String?, body: String): String {
        val header = listOfNotNull(stage, time, author, automated).filter { it.isNotEmpty() }.joinToString(Separator)
        return if (body.isEmpty()) header else header + BodySeparator + body
    }

    /** The update to announce when the list grows: the last one, only when there are more updates than before. */
    fun newUpdate(updates: List<MonarchIncidentUpdate>, previousCount: Int): MonarchIncidentUpdate? =
        if (updates.size > previousCount) updates.lastOrNull() else null
}
