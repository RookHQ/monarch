package dev.monarchui.components.intelligence.aitoolresultcard

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Where the undo of a `MonarchAiToolResultCard` is. */
internal enum class MonarchAiToolResultCardUndo {
    /** Undo is offered and has not run. */
    Idle,

    /** Undo is running. */
    Pending,

    /** Undo worked. */
    Done,

    /** Undo threw. */
    Failed,
}

/** The fill and the glyph colour of the icon tile. */
internal data class MonarchAiToolResultCardTileColors(val container: Color, val content: Color)

/** Component tokens and the pure logic of MonarchAiToolResultCard. */
internal object MonarchAiToolResultCardTokens {
    /** The gap between the icon tile and the text, and between the text and the actions. */
    val ContentGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of text. */
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the open link and the undo button. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The side of the icon tile. */
    val TileSize: Dp = MonarchTokens.Control.sm.dp

    /** The tile: success colours for a result, muted once undone. */
    fun tileColors(undone: Boolean, colors: MonarchColors): MonarchAiToolResultCardTileColors =
        if (undone) {
            MonarchAiToolResultCardTileColors(colors.bgMuted, colors.textMuted)
        } else {
            MonarchAiToolResultCardTileColors(colors.successSubtle, colors.successStrong)
        }

    /** The colour of the status line: muted for "Undone", the danger strong role for a failure. */
    fun statusColor(undo: MonarchAiToolResultCardUndo, colors: MonarchColors): Color =
        if (undo == MonarchAiToolResultCardUndo.Failed) colors.dangerStrong else colors.textMuted

    /** Whether Open and Undo are shown: they go away once the undo worked. */
    fun showsActions(undo: MonarchAiToolResultCardUndo): Boolean = undo != MonarchAiToolResultCardUndo.Done

    /** Whether a tap on Undo starts it: not while it runs and not once it worked. */
    fun canUndo(undo: MonarchAiToolResultCardUndo): Boolean =
        undo == MonarchAiToolResultCardUndo.Idle || undo == MonarchAiToolResultCardUndo.Failed

    /** The built-in label of a kind. */
    @StringRes
    fun kindLabel(kind: MonarchAiToolResultKind): Int = when (kind) {
        MonarchAiToolResultKind.RecordCreated -> R.string.monarch_aitoolresultcard_kind_record_created
        MonarchAiToolResultKind.EmailSent -> R.string.monarch_aitoolresultcard_kind_email_sent
        MonarchAiToolResultKind.EventBooked -> R.string.monarch_aitoolresultcard_kind_event_booked
        MonarchAiToolResultKind.TicketFiled -> R.string.monarch_aitoolresultcard_kind_ticket_filed
        MonarchAiToolResultKind.Generic -> R.string.monarch_aitoolresultcard_kind_generic
    }
}
