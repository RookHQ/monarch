package com.rookhq.monarch.components.intelligence.aiartifactversions

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The versions around the one shown, after sorting. */
internal class AiArtifactVersionsPlace(
    val sorted: List<MonarchAiArtifactVersion>,
    val index: Int,
) {
    val current: MonarchAiArtifactVersion? get() = sorted.getOrNull(index)
    val previous: MonarchAiArtifactVersion? get() = sorted.getOrNull(index - 1)
}

/** Component tokens and pure logic of MonarchAiArtifactVersions, built from primitives only. */
internal object MonarchAiArtifactVersionsTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The diff never grows past this height, so it can sit in a screen that scrolls. */
    val DiffMaxHeight: Dp = (MonarchTokens.Space.space24 * DIFF_HEIGHT_STEPS).dp

    private const val DIFF_HEIGHT_STEPS = 4

    /**
     * The versions oldest to newest, one per number (the last given wins), and
     * the place of [selection]; a number that is not there shows the newest.
     */
    fun place(versions: List<MonarchAiArtifactVersion>, selection: Int): AiArtifactVersionsPlace {
        val sorted = versions.associateBy { it.version }.values.sortedBy { it.version }
        val found = sorted.indexOfFirst { it.version == selection }
        return AiArtifactVersionsPlace(sorted, if (found >= 0) found else sorted.lastIndex)
    }

    /** Compare is offered when asked for, there is a previous version, and both have content. */
    fun canCompare(place: AiArtifactVersionsPlace, showCompare: Boolean): Boolean =
        showCompare && place.current?.content != null && place.previous?.content != null

    /** One menu item: "v3 · Shorter intro · Mar 12, 2026, 2:05 PM". */
    fun option(tag: String, label: String?, date: String): String =
        listOfNotNull(tag, label?.takeIf { it.isNotBlank() }, date.takeIf { it.isNotEmpty() }).joinToString(" · ")

    /** A medium date and a short time in [locale] and [timeZone]. */
    fun date(millis: Long, locale: Locale, timeZone: java.util.TimeZone): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        format.timeZone = TimeZone.getTimeZone(timeZone.id)
        return format.format(millis)
    }
}
