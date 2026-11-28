package com.rookhq.monarch.components.scheduling.eventchip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One event in a calendar view: a colour dot or bar, the title, the time and
 * the status, as a button that opens the event or as a label.
 *
 * Built by Monarch: Material 3 has no equivalent. The event colour is only
 * ever the marker, a border and a light tint; text keeps the Monarch text
 * colours. TalkBack reads the chip as one element, "title, time, status,
 * detail". A status other than confirmed is never shape or colour alone: a
 * hollow or dashed marker, a struck-through title for cancelled and declined
 * events, and the status word.
 *
 * With [onClick] the chip is a button with a 48dp touch target reserved in
 * the layout; the visible chip stays compact inside it.
 *
 * @param title the event title, already localised.
 * @param modifier the modifier of the chip.
 * @param variant `Dot`, `Bar` or `Block`.
 * @param status confirmed, tentative, cancelled or declined.
 * @param color the event's colour, for the marker, the border and the tint only. Unspecified is the
 * theme's accent.
 * @param past the event has ended: muted title and a faded marker. Not read by TalkBack; the time says
 * when the event was.
 * @param compact puts a `Block` chip on one line, time then title, without the status word and the detail.
 * @param enabled whether [onClick] can be triggered. The look does not change.
 * @param time the visible time, already formatted, such as "09:30" or "09:30 – 10:15". Leave it out for an
 * all-day event in an all-day row.
 * @param timeLabel the time as TalkBack reads it when [time] is abbreviated, such as "9:30 to 10:15" or
 * "All day".
 * @param detail a second, muted line in `Bar` and `Block` chips: a location, a service, an organiser.
 * @param statusLabel replaces the built-in name of the status (`monarch_eventchip_<status>`), in the visible
 * word and in what TalkBack reads.
 * @param onClick opens the event. With it the chip is a button; without it a label.
 * @param timeGutter in the `Bar` variant, puts the time in a fixed column at the start with the title, the
 * detail and the status word (in capitals, at the end) beside it, as an agenda lists events.
 * @param interactionSource the source of the press and focus interactions of the chip.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchEventChip(
    title: String,
    modifier: Modifier = Modifier,
    variant: MonarchEventChipVariant = MonarchEventChipVariant.Dot,
    status: MonarchEventChipStatus = MonarchEventChipStatus.Confirmed,
    color: Color = Color.Unspecified,
    past: Boolean = false,
    compact: Boolean = false,
    enabled: Boolean = true,
    time: String? = null,
    timeLabel: String? = null,
    detail: String? = null,
    statusLabel: String? = null,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    timeGutter: Boolean = false,
) {
    val tokens = MonarchEventChipTokens
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = tokens.colors(MonarchTheme.colors, color, variant, status, past, pressed && onClick != null)
    val statusName = statusLabel ?: stringResource(tokens.statusRes(status))
    val statusText = if (status == MonarchEventChipStatus.Confirmed) null else statusName
    val name = tokens.spokenName(title, timeLabel ?: time, statusText, detail, variant)
    val shape = if (variant == MonarchEventChipVariant.Dot) MonarchTheme.shapes.xs else MonarchTheme.shapes.sm
    val content = EventChipContent(title, time, statusText, detail, status, colors)

    val interaction = if (onClick != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
    } else {
        Modifier
    }
    val border = colors.border
    val outline = if (border != null) Modifier.border(MonarchShapeDefaults.BorderWidth, border, shape) else Modifier
    val surface = Modifier
        .clip(shape)
        .background(colors.container, shape)
        .then(outline)
    val semantics = if (onClick != null) {
        Modifier
            .clickable(interactions, ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name }
    } else {
        Modifier.semantics(mergeDescendants = true) { contentDescription = name }
    }
    val sized = if (variant == MonarchEventChipVariant.Block) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
    Box(modifier = modifier.then(interaction).then(sized).then(surface).then(semantics)) {
        // The texts are read once, through the name of the chip.
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            when (variant) {
                MonarchEventChipVariant.Dot -> DotLayout(content)
                MonarchEventChipVariant.Bar -> BarLayout(content, block = false, compact = false, gutter = timeGutter)
                MonarchEventChipVariant.Block -> BarLayout(content, block = true, compact = compact, gutter = false)
            }
        }
    }
}

/** What a chip shows, resolved once. */
private class EventChipContent(
    val title: String,
    val time: String?,
    val status: String?,
    val detail: String?,
    val state: MonarchEventChipStatus,
    val colors: EventChipColors,
) {
    val struck: Boolean get() = MonarchEventChipTokens.isInactive(state)
}

@Composable
private fun DotLayout(content: EventChipContent) {
    val tokens = MonarchEventChipTokens
    val fontScale = LocalDensity.current.fontScale
    val markerSize = tokens.DotSize * fontScale
    val wraps = fontScale >= tokens.WRAP_FONT_SCALE
    Row(
        modifier = Modifier.padding(horizontal = tokens.DotHorizontalPadding, vertical = tokens.DotVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val marker = content.colors.marker
        val hollow = tokens.isHollow(content.state)
        Box(
            modifier = Modifier.size(markerSize).drawBehind {
                val stroke = tokens.DotStroke.toPx()
                if (hollow) {
                    drawCircle(marker, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                } else {
                    drawCircle(marker)
                }
            },
        )
        content.time?.takeIf { it.isNotEmpty() }?.let {
            ChipText(it, secondaryStyle(), content.colors.secondary, singleLine = !wraps)
        }
        Text(
            text = content.title,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTheme.typography.caption,
            fontWeight = FontWeight(tokens.TITLE_WEIGHT),
            color = content.colors.title,
            textDecoration = if (content.struck) TextDecoration.LineThrough else null,
            maxLines = if (wraps) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarLayout(content: EventChipContent, block: Boolean, compact: Boolean, gutter: Boolean) {
    val tokens = MonarchEventChipTokens
    val marker = content.colors.marker
    val dashed = content.state == MonarchEventChipStatus.Tentative
    val title = @Composable { ChipTitle(content, block) }
    val time = @Composable {
        content.time?.takeIf { it.isNotEmpty() }?.let { ChipText(it, secondaryStyle(), content.colors.secondary) }
    }
    val status = @Composable { content.status?.let { ChipText(it, statusStyle(), content.colors.secondary) } }
    val detail = @Composable { content.detail?.let { ChipText(it, secondaryStyle(), content.colors.secondary) } }
    Box(
        modifier = Modifier
            .then(if (block) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .drawBehind { drawBar(marker, dashed) }
            .padding(
                start = tokens.BarHorizontalPadding + tokens.BarWidth,
                end = tokens.BarHorizontalPadding,
                top = if (compact) tokens.DotVerticalPadding else tokens.BarVerticalPadding,
                bottom = if (compact) tokens.DotVerticalPadding else tokens.BarVerticalPadding,
            ),
    ) {
        when {
            compact -> Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.DotHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                time()
                title()
            }
            gutter -> Row(horizontalArrangement = Arrangement.spacedBy(tokens.RowGap * 2)) {
                Box(modifier = Modifier.width(tokens.GutterWidth)) { time() }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) { title() }
                        content.status?.let {
                            ChipText(it.uppercase(), statusStyle(), content.colors.secondary)
                        }
                    }
                    detail()
                }
            }
            block -> Column {
                title()
                time()
                status()
                detail()
            }
            else -> Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
                    title()
                    time()
                    status()
                }
                detail()
            }
        }
    }
}

@Composable
private fun ChipTitle(content: EventChipContent, block: Boolean) {
    val tokens = MonarchEventChipTokens
    Text(
        text = content.title,
        style = MonarchTheme.typography.caption,
        fontWeight = FontWeight(if (block) tokens.BLOCK_TITLE_WEIGHT else tokens.TITLE_WEIGHT),
        color = content.colors.title,
        textDecoration = if (content.struck) TextDecoration.LineThrough else null,
        // A one-word title in a narrow block is cut with an ellipsis, never broken in the middle of the word.
        softWrap = !block || content.title.contains(' '),
        overflow = if (block) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
private fun ChipText(text: String, style: TextStyle, color: Color, singleLine: Boolean = false) {
    Text(
        text = text,
        style = style,
        color = color,
        softWrap = !singleLine,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
private fun secondaryStyle(): TextStyle = MonarchTheme.typography.caption.copy(fontFeatureSettings = TabularFigures)

@Composable
private fun statusStyle(): TextStyle =
    MonarchTheme.typography.caption.copy(fontWeight = FontWeight(MonarchEventChipTokens.STATUS_WEIGHT))

/** The colour bar at the start edge: solid, or dashed for a tentative event. Mirrors right to left. */
private fun DrawScope.drawBar(color: Color, dashed: Boolean) {
    val width = MonarchEventChipTokens.BarWidth.toPx()
    val x = MonarchEventChipTokens.barStart(size.width, width, layoutDirection)
    if (!dashed) {
        drawRect(color, topLeft = Offset(x, 0f), size = Size(width, size.height))
        return
    }
    val dash = MonarchEventChipTokens.BarDash.toPx()
    val step = dash + MonarchEventChipTokens.BarGap.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(color, topLeft = Offset(x, y), size = Size(width, minOf(dash, size.height - y)))
        y += step
    }
}

private const val TabularFigures = "tnum"
