package com.rookhq.monarch.components.collaboration.decisioncard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and rules of MonarchDecisionCard, built from primitives and roles only. */
internal object MonarchDecisionCardTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val SmallGap: Dp = MonarchTokens.Space.space1.dp

    /** The default longest reason, in characters. */
    const val DEFAULT_REASON_LIMIT: Int = 500

    /** The number of characters left is shown from this many on. */
    const val CHARACTERS_LEFT_THRESHOLD: Int = 50

    /** The most decision tiles shown side by side. */
    const val MAX_TILE_COLUMNS: Int = 4

    /** The colour of secondary text: the group name, the note, the time and the saved message. */
    fun mutedText(colors: MonarchColors): Color = colors.textMuted

    /** Whether [choice] needs a reason. "No decision" never does; an option's own rule wins. */
    fun needsReason(choice: String?, options: List<MonarchDecisionOption>, reasonRequired: Boolean): Boolean =
        choice != null && (options.firstOrNull { it.value == choice }?.reasonRequired ?: reasonRequired)

    /** Whether the form differs from the saved decision. White space around the reason does not count. */
    fun dirty(choice: String?, text: String, value: String?, reason: String?): Boolean =
        choice != value || text.trim() != reason.orEmpty().trim()

    /** Whether the count of characters left is shown. */
    fun showsCharactersLeft(limit: Int, length: Int): Boolean = limit - length <= CHARACTERS_LEFT_THRESHOLD

    /** The decision passed to `onDecide`: the reason trimmed, and `null` when empty. */
    fun decision(choice: String?, text: String): MonarchDecision =
        MonarchDecision(value = choice, reason = text.trim().ifEmpty { null })

    /** "3 hours ago" in the user's locale, by the shared Monarch rule (the web rule). */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)
}
