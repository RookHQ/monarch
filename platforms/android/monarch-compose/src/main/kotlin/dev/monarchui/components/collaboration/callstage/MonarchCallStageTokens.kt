package dev.monarchui.components.collaboration.callstage

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** A button in the header of a call stage. */
internal enum class CallStageTool { PictureInPicture, Shrink, Expand, Minimize }

/** Component tokens and the pure rules of MonarchCallStage. */
internal object MonarchCallStageTokens {
    /** The space between tiles and between the parts of the stage: wider on the full stage. */
    fun gap(compact: Boolean): Dp = if (compact) MonarchTokens.Space.space1.dp else MonarchTokens.Space.space2.dp

    /** The space between the header's parts. */
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp

    /** The width of a filmstrip tile under the main tile. */
    val StripTile: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp

    /** The width of the filmstrip column beside the main tile on a wide stage. */
    val StripColumn: Dp = (MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space2).dp

    /** Below this width the grid has at most two columns, as on the web (30rem). */
    val NarrowWidth: Dp = (MonarchTokens.Space.space24 * 5).dp

    /** From this width the full stage puts the filmstrip beside the main tile. */
    val WideWidth: Dp = (MonarchTokens.Space.space24 * 8 + MonarchTokens.Space.space12).dp

    private const val COMPACT_COLUMNS = 2
    private const val SECOND = 1_000L
    private const val MINUTE = 60L
    private const val HOUR = 3_600L

    /** Whether a surface is compact: docked or picture in picture. */
    fun compact(surface: MonarchCallStageSurface): Boolean = surface != MonarchCallStageSurface.Stage

    /** The columns drawn: the layout's, at most two on a compact surface or a narrow screen. */
    fun columns(layout: Int, compact: Boolean, narrow: Boolean): Int =
        if (compact || narrow) minOf(layout, COMPACT_COLUMNS).coerceAtLeast(1) else layout.coerceAtLeast(1)

    /** The header buttons, in order, for a surface and the callbacks the app gave. */
    fun tools(
        surface: MonarchCallStageSurface,
        pictureInPicture: Boolean,
        surfaceChange: Boolean,
        minimize: Boolean,
    ): List<CallStageTool> = listOfNotNull(
        CallStageTool.PictureInPicture.takeIf { pictureInPicture && surface != MonarchCallStageSurface.Pip },
        CallStageTool.Shrink.takeIf { surfaceChange && surface == MonarchCallStageSurface.Stage },
        CallStageTool.Expand.takeIf { surfaceChange && surface == MonarchCallStageSurface.Docked },
        CallStageTool.Minimize.takeIf { minimize && surface == MonarchCallStageSurface.Docked },
    )

    /** Whether you are alone: no participant other than yourself. */
    fun waiting(participants: List<MonarchCallStageParticipant>): Boolean = participants.none { !it.self }

    /** The running time since [startedAt], `mm:ss` or `h:mm:ss` after an hour; never negative. */
    fun duration(startedAt: Long, now: Long, locale: Locale): String {
        val seconds = maxOf(0L, (now - startedAt) / SECOND)
        val hours = seconds / HOUR
        val minutes = seconds % HOUR / MINUTE
        val rest = seconds % MINUTE
        return if (hours > 0) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(locale, "%02d:%02d", minutes, rest)
        }
    }
}
