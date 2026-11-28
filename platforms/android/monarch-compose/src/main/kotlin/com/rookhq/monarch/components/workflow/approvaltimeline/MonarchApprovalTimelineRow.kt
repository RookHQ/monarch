package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/** The glyphs on a 16-unit grid. */
private const val Grid = 16f
private const val GlyphStroke = 2f
private const val DotRadius = 3f
private val CheckPoints = listOf(Offset(4f, 8.5f), Offset(7f, 11.5f), Offset(12f, 5f))
private val Cross = listOf(Offset(5f, 5f) to Offset(11f, 11f), Offset(11f, 5f) to Offset(5f, 11f))
private val DashLine = Offset(5f, 8f) to Offset(11f, 8f)

/**
 * One step: its marker on the rail, the label and status, who decided and when, who decides next, the comment and the
 * note. Read by TalkBack as one element.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchApprovalTimelineRow(
    step: MonarchApprovalTimelineStep,
    context: MonarchApprovalTimelineContext,
    last: Boolean,
) {
    val colors = MonarchTheme.colors
    val strings = context.strings
    val tokens = MonarchApprovalTimelineTokens
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rail = tokens.rail(step.status, colors)
    val status = strings.status(step.status)
    val approvers = MonarchApprovalTimelineText.approversLine(step, strings::approvers, strings::delegatedFrom)
    val delegation = MonarchApprovalTimelineText.delegationLine(step, strings::delegatedFrom)
    val spoken = MonarchApprovalTimelineText.spoken(
        step,
        status,
        strings.current,
        step.date?.let { MonarchApprovalTimelineText.absoluteTime(it, context.zone, context.locale) },
        listOf(approvers, delegation),
    )
    val side = context.markerSize
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (last) return@drawBehind
                val x = if (rtl) size.width - side.toPx() / 2 else side.toPx() / 2
                val gap = tokens.RailGap.toPx()
                val top = Offset(x, side.toPx() + gap)
                drawLine(rail, top, Offset(x, size.height - gap), tokens.Rail.toPx(), StrokeCap.Round)
            }
            .padding(bottom = if (last) tokens.LineGap else tokens.StepGap)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(tokens.ColumnGap),
    ) {
        Marker(tokens.marker(step.status, colors), side)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.MetaGap * 2),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(
                    text = step.label,
                    style = MonarchTextStyle.BodyStrong,
                    color = tokens.labelColor(step.status, colors),
                )
                MonarchText(
                    text = buildAnnotatedString {
                        val weight = if (tokens.emphasised(step.status)) FontWeight.SemiBold else FontWeight.Normal
                        withStyle(SpanStyle(fontWeight = weight)) { append(status) }
                    },
                    style = MonarchTextStyle.Caption,
                    color = tokens.statusColor(step.status, colors),
                )
            }
            if (MonarchApprovalTimelineText.decided(step.status) && (step.actor != null || step.date != null)) {
                DecisionLine(step, context, colors)
            }
            approvers?.let { Meta(it, colors) }
            delegation?.let { Meta(it, colors) }
            step.comment?.takeIf { it.isNotEmpty() }?.let { comment ->
                MonarchText(
                    text = comment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.comment(colors), RoundedCornerShape(tokens.CommentRadius))
                        .padding(horizontal = tokens.CommentHorizontal, vertical = tokens.CommentVertical),
                    style = MonarchTextStyle.Caption,
                )
            }
            step.note?.takeIf { it.isNotEmpty() }?.let { Meta(it, colors) }
        }
    }
}

/** Who decided, with a decorative avatar, a dot and when. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionLine(
    step: MonarchApprovalTimelineStep,
    context: MonarchApprovalTimelineContext,
    colors: MonarchColors,
) {
    val tokens = MonarchApprovalTimelineTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.MetaGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val actor = step.actor
        if (actor != null) {
            if (context.showAvatars) {
                MonarchAvatar(
                    image = actor.image,
                    name = actor.name,
                    size = MonarchAvatarSize.Sm,
                    seed = actor.seed,
                    decorative = true,
                )
            }
            Meta(actor.name, colors)
        }
        val date = step.date
        if (actor != null && date != null) Meta(tokens.MetaDot, colors)
        if (date != null) {
            val time = MonarchApprovalTimelineText.timeText(
                date, context.now, context.absoluteAfterMillis, context.zone, context.locale,
            )
            Meta(time, colors)
        }
    }
}

@Composable
private fun Meta(text: String, colors: MonarchColors) {
    MonarchText(text = text, style = MonarchTextStyle.Caption, color = MonarchApprovalTimelineTokens.muted(colors))
}

/** The marker: a circle with its outline, solid or dashed, and its glyph. Decoration. */
@Composable
private fun Marker(marker: MonarchApprovalTimelineMarker, side: Dp) {
    val tokens = MonarchApprovalTimelineTokens
    Canvas(modifier = Modifier.size(side).clearAndSetSemantics { }) {
        val border = tokens.MarkerBorder.toPx()
        val radius = size.minDimension / 2
        drawCircle(marker.fill, radius)
        val dash = tokens.Dash.toPx()
        drawCircle(
            marker.border,
            radius - border / 2,
            style = Stroke(
                width = border,
                pathEffect = if (marker.dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
            ),
        )
        drawGlyph(marker.shape, marker.glyph)
    }
}

private fun DrawScope.drawGlyph(glyph: MonarchApprovalTimelineGlyph, color: Color) {
    val unit = size.width / Grid
    val width = GlyphStroke * unit
    when (glyph) {
        MonarchApprovalTimelineGlyph.Check -> CheckPoints.zipWithNext().forEach { (from, to) ->
            drawLine(color, from * unit, to * unit, width, StrokeCap.Round)
        }
        MonarchApprovalTimelineGlyph.Cross -> Cross.forEach { (from, to) ->
            drawLine(color, from * unit, to * unit, width, StrokeCap.Round)
        }
        MonarchApprovalTimelineGlyph.Dot -> drawCircle(color, DotRadius * unit)
        MonarchApprovalTimelineGlyph.Dash ->
            drawLine(color, DashLine.first * unit, DashLine.second * unit, width, StrokeCap.Round)
        MonarchApprovalTimelineGlyph.None -> Unit
    }
}
