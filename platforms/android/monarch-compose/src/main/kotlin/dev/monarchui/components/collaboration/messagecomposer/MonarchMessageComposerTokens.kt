package dev.monarchui.components.collaboration.messagecomposer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** A mention picked in the composer, with the trigger it was typed with. */
internal data class MessageComposerPicked(val trigger: Char, val item: MonarchMentionItem) {
    /** How it is written in the text: "@Ana Ruiz". */
    val written: String get() = "$trigger${item.label}"
}

/** Component tokens and pure logic of MonarchMessageComposer, built from primitives and roles only. */
internal object MonarchMessageComposerTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val InnerGap: Dp = MonarchTokens.Space.space2.dp

    /** The card: 12dp on every side, so the top and the bottom match the sides. */
    val BoxPadding: PaddingValues = PaddingValues(MonarchTokens.Space.space3.dp)

    /** The input's own box inside the card: 12dp at the sides, 10dp above and below one line of 14sp text. */
    val FieldPadding: PaddingValues = PaddingValues(
        horizontal = MonarchTokens.Space.space3.dp,
        vertical = MonarchTokens.Space.space2_5.dp,
    )

    /** The least height of the input box. */
    val FieldMinHeight: Dp = MonarchTokens.Space.space12.dp

    /** The least time between two onTyping calls by default, as on the web and iOS. */
    const val TYPING_INTERVAL_MILLIS: Long = 3_000L

    /** The text grows to this many lines, then scrolls. */
    const val MAX_LINES: Int = 8

    /**
     * Whether the message can be sent: enabled, not already sending, with
     * text or attachments, and not over [maxLength].
     */
    fun canSend(
        text: CharSequence,
        enabled: Boolean,
        sending: Boolean,
        hasAttachments: Boolean,
        maxLength: Int?,
    ): Boolean = enabled && !sending && (text.isNotBlank() || hasAttachments) && !isOver(text, maxLength)

    /** The characters left before [maxLength]; negative when over, `null` without a limit. */
    fun remaining(text: CharSequence, maxLength: Int?): Int? = maxLength?.let { it - text.length }

    /** Whether [text] is longer than [maxLength]. */
    fun isOver(text: CharSequence, maxLength: Int?): Boolean = (remaining(text, maxLength) ?: 0) < 0

    /** The mentions still written in [text], each once, in the order they were picked. */
    fun mentionsIn(text: String, picked: List<MessageComposerPicked>): List<MonarchMentionItem> =
        picked.filter { text.contains(it.written) }.map { it.item }.distinctBy { it.id }

    /**
     * [text] with the query from [start] to [end] replaced by [written] and a
     * space, and the caret after that space. A space already after the query
     * is reused, so a mention typed mid-sentence does not leave two.
     */
    fun insertMention(text: String, start: Int, end: Int, written: String): Pair<String, Int> {
        if (start < 0 || end > text.length || start > end) return text to text.length
        val after = text.substring(end)
        val replacement = if (after.startsWith(' ')) written else "$written "
        return text.substring(0, start) + replacement + after to start + written.length + 1
    }

    /** The box: muted when disabled, the warning tint for a note, else the surface with the input border. */
    fun boxColors(note: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderSubtle)
        note -> MonarchSurfaceColors(colors.warningSubtle, colors.textDefault, colors.warningBorder)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderInput)
    }

    /** The placeholder colour. */
    fun placeholderColor(colors: MonarchColors): Color = colors.textSubtle

    /** The count colour: the strong danger colour over the limit, muted otherwise. */
    fun countColor(over: Boolean, colors: MonarchColors): Color = if (over) colors.dangerStrong else colors.textMuted

    /** The colour of the context bar's excerpt and the disabled reason. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The caret colour. */
    fun cursorColor(colors: MonarchColors): Color = colors.accentSolid
}
