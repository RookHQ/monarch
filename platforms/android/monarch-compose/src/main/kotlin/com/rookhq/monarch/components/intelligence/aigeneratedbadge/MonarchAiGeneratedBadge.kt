package com.rookhq.monarch.components.intelligence.aigeneratedbadge

import android.icu.text.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date

/**
 * A small badge that marks content as written by AI, written with AI help, or edited by a person after AI
 * wrote it.
 *
 * Built by Monarch: Material 3 has no equivalent, and `MonarchBadge` has no
 * AI colours and no dashed border. A capsule in the AI colours with a text
 * label; the edited kind has a dashed border. The model and the time, when
 * given, are read by TalkBack after the label ("AI generated, By
 * gemini-2.5-pro, 3 Mar 2025, 14:05"); touch has no tooltip, so they are never
 * shown and must not be essential. The badge is one TalkBack element and is
 * not interactive.
 *
 * @param modifier the modifier of the badge.
 * @param kind how much of the content came from AI.
 * @param model the model that produced the content, for example `gemini-2.5-pro`. Part of the provenance
 * line. Blank text counts as no model.
 * @param dateMillis when the content was produced, in epoch milliseconds (UTC). Part of the provenance
 * line, as a medium date and short time in the device time zone.
 * @param size the size.
 * @param iconOnly shows only [icon]; TalkBack still reads the label. Ignored without an icon, because the
 * badge would be empty.
 * @param icon the AI mark before the label, for example a `MonarchIcon` of a bot with a `null` content
 * description. Drawn by default, as on the web; `null` draws none. It takes the strong AI colour as its content colour.
 * @param label replaces the built-in label of the kind, for example `monarch_aigeneratedbadge_generated`.
 * @param detail replaces the built-in provenance line.
 */
@Composable
public fun MonarchAiGeneratedBadge(
    modifier: Modifier = Modifier,
    kind: MonarchAiGeneratedBadgeKind = MonarchAiGeneratedBadgeKind.Generated,
    model: String? = null,
    dateMillis: Long? = null,
    size: MonarchAiGeneratedBadgeSize = MonarchAiGeneratedBadgeSize.Md,
    iconOnly: Boolean = false,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    label: String? = null,
    detail: String? = null,
) {
    val text = monarchString(label, kindResource(kind))
    val provenance = detail ?: provenanceLine(model, dateMillis)
    val spoken = if (provenance.isNullOrEmpty()) {
        text
    } else {
        stringResource(R.string.monarch_aigeneratedbadge_described, text, provenance)
    }
    val colors = MonarchAiGeneratedBadgeTokens.colors(MonarchTheme.colors)
    val showsIconOnly = iconOnly && icon != null
    val shape = MonarchShapeDefaults.Mark
    val dashed = MonarchAiGeneratedBadgeTokens.dashed(kind)
    val border = colors.border ?: Color.Transparent
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = spoken }
            .background(colors.container, shape)
            .then(
                if (dashed) {
                    Modifier.dashedCapsule(border)
                } else {
                    Modifier.border(MonarchAiGeneratedBadgeTokens.BorderWidth, border, shape)
                },
            )
            .heightIn(min = MonarchAiGeneratedBadgeTokens.minHeight(size))
            .padding(horizontal = MonarchAiGeneratedBadgeTokens.horizontalPadding(size, showsIconOnly)),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiGeneratedBadgeTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(MonarchAiGeneratedBadgeTokens.iconSize(size)),
                    contentAlignment = Alignment.Center,
                ) { icon() }
            }
            if (!showsIconOnly) {
                Text(
                    text = text,
                    style = MonarchTheme.typography.caption,
                    fontWeight = FontWeight(MonarchAiGeneratedBadgeTokens.FONT_WEIGHT),
                    color = colors.content,
                )
            }
        }
    }
}

private fun kindResource(kind: MonarchAiGeneratedBadgeKind): Int = when (kind) {
    MonarchAiGeneratedBadgeKind.Generated -> R.string.monarch_aigeneratedbadge_generated
    MonarchAiGeneratedBadgeKind.Assisted -> R.string.monarch_aigeneratedbadge_assisted
    MonarchAiGeneratedBadgeKind.Edited -> R.string.monarch_aigeneratedbadge_edited
}

@Composable
private fun provenanceLine(model: String?, dateMillis: Long?): String? {
    val locale = LocalConfiguration.current.locales[0]
    val at = remember(dateMillis, locale) {
        dateMillis?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(it)) }
    }
    val shownModel = MonarchAiGeneratedBadgeTokens.shownModel(model)
    return when {
        shownModel != null && at != null ->
            stringResource(R.string.monarch_aigeneratedbadge_by_model_at, shownModel, at)
        shownModel != null -> stringResource(R.string.monarch_aigeneratedbadge_by_model, shownModel)
        else -> at
    }
}

/** A dashed capsule outline, for the edited kind. */
private fun Modifier.dashedCapsule(color: Color): Modifier = drawBehind {
    val width = MonarchAiGeneratedBadgeTokens.BorderWidth.toPx()
    val dash = MonarchAiGeneratedBadgeTokens.Dash.toPx()
    val inset = width / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(size.height / 2),
        style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}
