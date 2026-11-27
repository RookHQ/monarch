package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.datadisplay.tag.MonarchTagSize
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One event: the marker on the day's line, the sentence with the actor and target in bold, the time and tags, the
 * body, and the details behind a disclosure. The sentence, time, tags and body are one TalkBack element; the
 * disclosure is its own.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchActivityTimelineRow(
    event: MonarchActivityTimelineEvent,
    text: MonarchActivityTimelineRowText,
    marker: MonarchActivityTimelineMarker,
    density: MonarchActivityTimelineDensity,
    last: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val colors = MonarchTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val line = MonarchActivityTimelineTokens.line(colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (last) return@drawBehind
                val markerPx = MonarchActivityTimelineTokens.Marker.toPx()
                val inset = MonarchActivityTimelineTokens.LineInset.toPx()
                val x = if (rtl) size.width - markerPx / 2 else markerPx / 2
                drawLine(
                    line, Offset(x, markerPx + inset), Offset(x, size.height - inset),
                    MonarchActivityTimelineTokens.Line.toPx(),
                )
            }
            .padding(bottom = MonarchActivityTimelineTokens.bottomGap(density, last)),
        horizontalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.MarkerGap),
    ) {
        Marker(event, marker)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap),
        ) {
            EventText(event, text)
            val details = event.details
            if (details != null || event.detailRows.isNotEmpty()) {
                MonarchCollapsible(
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    label = if (expanded) text.hideDetails else text.details,
                    // The chevron is what tells a reader that "Details" opens; the collapsible turns it over.
                    trailingIcon = { MonarchFieldGlyphIcon(MonarchFieldGlyph.ChevronDown) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonarchTheme.colors.bgSubtle, MonarchTheme.shapes.md)
                            .padding(MonarchActivityTimelineTokens.BodyPadding),
                    ) {
                        if (details != null) MonarchText(text = details, style = MonarchTextStyle.Caption)
                        ActivityDetailRows(event.detailRows)
                    }
                }
            }
        }
    }
}

/** The sentence, the time and tags, and the body, read as one element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventText(event: MonarchActivityTimelineEvent, text: MonarchActivityTimelineRowText) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = text.spoken },
        verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap),
    ) {
        val sentence = buildAnnotatedString {
            event.actor?.let {
                append(it.name)
                append(" ")
            }
            // As the web: the verb is quieter than the people and things it joins.
            withStyle(SpanStyle(color = colors.textMuted)) { append(event.verb) }
            event.target?.let {
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(it) }
            }
        }
        MonarchText(text = sentence)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap),
            verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(
                text = text.relative,
                style = MonarchTextStyle.Caption,
                color = MonarchActivityTimelineTokens.time(colors),
            )
            text.automated?.let { MonarchTag(text = it, size = MonarchTagSize.Sm) }
            event.tags.forEach { MonarchTag(text = it, size = MonarchTagSize.Sm) }
        }
        event.body?.let {
            MonarchText(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(MonarchActivityTimelineTokens.BodyBorder, colors.borderDefault, MonarchTheme.shapes.md)
                    .padding(MonarchActivityTimelineTokens.BodyPadding),
            )
        }
    }
}

/** The marker: the event's icon, the actor's avatar or a dot in the tone. Decoration. */
@Composable
private fun Marker(event: MonarchActivityTimelineEvent, marker: MonarchActivityTimelineMarker) {
    val palette = MonarchActivityTimelineTokens.markerColors(event.tone, MonarchTheme.colors)
    val size = Modifier.size(MonarchActivityTimelineTokens.Marker).clearAndSetSemantics { }
    val icon = event.icon
    val actor = event.actor
    when {
        icon != null -> Box(
            modifier = size
                .border(MonarchActivityTimelineTokens.Line, palette.outline, CircleShape)
                .background(palette.ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) { CompositionLocalProvider(LocalContentColor provides palette.icon) { icon() } }
        marker == MonarchActivityTimelineMarker.Avatar && actor != null -> MonarchAvatar(
            image = actor.image,
            name = actor.name,
            modifier = size,
            size = MonarchAvatarSize.Sm,
            seed = actor.seed,
            decorative = true,
        )
        else -> Canvas(size) {
            val dot = MonarchActivityTimelineTokens.Dot.toPx() / 2
            val ring = MonarchActivityTimelineTokens.DotRing.toPx()
            val hairline = MonarchActivityTimelineTokens.Line.toPx()
            drawCircle(palette.outline, dot + ring + hairline)
            drawCircle(palette.ring, dot + ring)
            drawCircle(palette.dot, dot)
        }
    }
}

/** The field and value lines of an event, in two aligned columns: the fields share the width of the longest one. */
@Composable
private fun ActivityDetailRows(rows: List<MonarchActivityTimelineDetail>) {
    if (rows.isEmpty()) return
    val muted = MonarchTheme.colors.textMuted
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.BodyPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap)) {
            rows.forEach { MonarchText(text = it.label, style = MonarchTextStyle.Caption, color = muted) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap)) {
            rows.forEach { MonarchText(text = it.value, style = MonarchTextStyle.Caption) }
        }
    }
}
