package dev.monarchui.components.workflow.approvaltimeline

import android.text.format.DateUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The glyph inside a step marker. */
internal enum class MonarchApprovalTimelineGlyph { Check, Cross, Dot, Dash, None }

/**
 * The marker of a step.
 *
 * @property fill the inside of the circle.
 * @property border the outline.
 * @property glyph the colour of the glyph.
 * @property shape the glyph.
 * @property dashed whether the outline is dashed.
 */
internal data class MonarchApprovalTimelineMarker(
    val fill: Color,
    val border: Color,
    val glyph: Color,
    val shape: MonarchApprovalTimelineGlyph,
    val dashed: Boolean,
)

/** Component tokens and the pure rules of MonarchApprovalTimeline. */
internal object MonarchApprovalTimelineTokens {
    /** The gap between the marker column and the step. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The space after each step but the last. */
    val StepGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the lines of one step. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the items of the meta line. */
    val MetaGap: Dp = MonarchTokens.Space.space1.dp

    /** The side of the marker at font scale 1. */
    val Marker: Dp = MonarchTokens.Space.space5.dp

    /** The outline of the marker. */
    val MarkerBorder: Dp = (MonarchTokens.Space.px * BORDER_SCALE).dp

    /** The dash and gap of a skipped or cancelled marker's outline. */
    val Dash: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of the rail between markers. */
    val Rail: Dp = (MonarchTokens.Space.px * 2).dp

    /** The gap between a marker and the rail below it. */
    val RailGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside a comment. */
    val CommentHorizontal: Dp = MonarchTokens.Space.space2_5.dp
    val CommentVertical: Dp = MonarchTokens.Space.space1_5.dp

    /** The corner radius of a comment. */
    val CommentRadius: Dp = MonarchTokens.Radius.md.dp

    /** By default, times further than seven days from now are shown as a date. */
    const val DefaultAbsoluteAfterMillis: Long = 7 * DateUtils.DAY_IN_MILLIS

    /** The dot between the actor and the time. */
    const val MetaDot: String = "·"

    private const val BORDER_SCALE = 1.5f

    /** The marker side for a font scale: the base size, larger when the user enlarges text, never smaller. */
    fun markerSize(fontScale: Float): Dp = Marker * fontScale.coerceAtLeast(1f)

    /**
     * The marker of [status]: a check, a cross, a dot, a dash or an empty circle, so the status is told by shape as
     * well as colour.
     */
    fun marker(status: MonarchApprovalStepStatus, colors: MonarchColors): MonarchApprovalTimelineMarker =
        when (status) {
            MonarchApprovalStepStatus.Submitted, MonarchApprovalStepStatus.Approved -> MonarchApprovalTimelineMarker(
                colors.successSolid,
                colors.successSolid,
                colors.successOnSolid,
                MonarchApprovalTimelineGlyph.Check,
                dashed = false,
            )
            MonarchApprovalStepStatus.Rejected -> MonarchApprovalTimelineMarker(
                colors.dangerSolid, colors.dangerSolid, colors.dangerOnSolid, MonarchApprovalTimelineGlyph.Cross, false,
            )
            MonarchApprovalStepStatus.Pending -> MonarchApprovalTimelineMarker(
                colors.warningSubtle,
                colors.warningSolid,
                colors.warningStrong,
                MonarchApprovalTimelineGlyph.Dot,
                dashed = false,
            )
            MonarchApprovalStepStatus.Waiting -> MonarchApprovalTimelineMarker(
                colors.bgSurface, colors.borderStrong, colors.textMuted, MonarchApprovalTimelineGlyph.None, false,
            )
            MonarchApprovalStepStatus.Skipped -> MonarchApprovalTimelineMarker(
                colors.bgSurface, colors.borderStrong, colors.textMuted, MonarchApprovalTimelineGlyph.Dash, true,
            )
            MonarchApprovalStepStatus.Cancelled -> MonarchApprovalTimelineMarker(
                colors.bgSurface, colors.borderStrong, colors.textMuted, MonarchApprovalTimelineGlyph.Cross, true,
            )
        }

    /** The rail below a step: the success border after a completed step, the subtle border otherwise. */
    fun rail(status: MonarchApprovalStepStatus, colors: MonarchColors): Color =
        if (status == MonarchApprovalStepStatus.Approved || status == MonarchApprovalStepStatus.Submitted) {
            colors.successBorder
        } else {
            colors.borderSubtle
        }

    /** The colour of the status text: strong danger when rejected, strong warning when pending, muted otherwise. */
    fun statusColor(status: MonarchApprovalStepStatus, colors: MonarchColors): Color = when (status) {
        MonarchApprovalStepStatus.Rejected -> colors.dangerStrong
        MonarchApprovalStepStatus.Pending -> colors.warningStrong
        else -> colors.textMuted
    }

    /** Whether the status text is bold: rejected and pending steps. */
    fun emphasised(status: MonarchApprovalStepStatus): Boolean =
        status == MonarchApprovalStepStatus.Rejected || status == MonarchApprovalStepStatus.Pending

    /** The colour of a step's label: muted for steps that have not happened. */
    fun labelColor(status: MonarchApprovalStepStatus, colors: MonarchColors): Color =
        if (status == MonarchApprovalStepStatus.Waiting || status == MonarchApprovalStepStatus.Skipped) {
            colors.textMuted
        } else {
            colors.textDefault
        }

    /** The meta lines and the time: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The fill behind a comment: the muted background. */
    fun comment(colors: MonarchColors): Color = colors.bgMuted
}
