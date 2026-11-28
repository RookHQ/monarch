package dev.monarchui.components.intelligence.aieditproposal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.diffviewer.MonarchDiffViewerLineKind
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of one kind of text in a change. */
internal data class MonarchAiEditProposalSpanColors(
    val text: Color,
    val background: Color,
    val decoration: TextDecoration?,
)

/** Component tokens and the pure logic of MonarchAiEditProposal. */
internal object MonarchAiEditProposalTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp
    val HunkPadding: Dp = MonarchTokens.Space.space3.dp
    val HunkGap: Dp = MonarchTokens.Space.space2.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp
    val HairlineWidth: Dp = MonarchTokens.Space.px.dp
    val AccentWidth: Dp = (MonarchTokens.Space.px * ACCENT_PX).dp
    private const val ACCENT_PX = 3

    /** The card: the default border, the AI border as the start accent, on the surface. */
    fun cardBorder(colors: MonarchColors): Color = colors.borderDefault

    /** The start accent of the card. */
    fun accent(colors: MonarchColors): Color = colors.aiBorder

    /** The fill of the card. */
    fun cardContainer(colors: MonarchColors): Color = colors.bgSurface

    /** The fill of a change. */
    fun hunkContainer(colors: MonarchColors): Color = colors.bgSubtle

    /** The border of a change. */
    fun hunkBorder(colors: MonarchColors): Color = colors.borderSubtle

    /** The title and the text of a change. */
    fun text(colors: MonarchColors): Color = colors.textDefault

    /** The count line, the label of a change and "(No change)". */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a change's decision: strong success when accepted, muted when rejected. */
    fun statusColor(status: MonarchAiEditHunkStatus, colors: MonarchColors): Color =
        if (status == MonarchAiEditHunkStatus.Accepted) colors.successStrong else colors.textMuted

    /**
     * The colours of removed or added text in a change with [status]. Undecided: the AI diff colours, struck
     * through or underlined. Accepted: removed text is subtle, added text plain. Rejected: added text is subtle
     * and removed text plain without the strike. Added text keeps its underline, so it never depends on colour.
     */
    fun spanColors(
        kind: MonarchDiffViewerLineKind,
        status: MonarchAiEditHunkStatus,
        colors: MonarchColors,
    ): MonarchAiEditProposalSpanColors {
        val removed = kind == MonarchDiffViewerLineKind.Removed
        val loser = (removed && status == MonarchAiEditHunkStatus.Accepted) ||
            (!removed && status == MonarchAiEditHunkStatus.Rejected)
        val winner = status != MonarchAiEditHunkStatus.Proposed && !loser
        return when {
            loser -> MonarchAiEditProposalSpanColors(
                colors.textSubtle,
                Color.Transparent,
                if (removed) TextDecoration.LineThrough else TextDecoration.Underline,
            )
            winner -> MonarchAiEditProposalSpanColors(
                colors.textDefault,
                Color.Transparent,
                if (removed) null else TextDecoration.Underline,
            )
            removed -> MonarchAiEditProposalSpanColors(
                colors.aiDiffRemoveStrong,
                colors.aiDiffRemove,
                TextDecoration.LineThrough,
            )
            else -> MonarchAiEditProposalSpanColors(colors.aiDiffAddStrong, colors.aiDiffAdd, TextDecoration.Underline)
        }
    }
}
