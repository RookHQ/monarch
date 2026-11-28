package com.rookhq.monarch.components.datadisplay.statcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicator
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricChange
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricFormat
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.components.overlays.popover.MonarchPopoverSide
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * One KPI for a dashboard: a label, a value, its change against an earlier
 * period and an optional hint, help text, trend line and drill-down, on a
 * card.
 *
 * Built by Monarch from a Material 3 `Surface` (clickable when [onClick] is
 * given), `Text` and `MonarchDeltaIndicator`; help opens a Material 3
 * `RichTooltip` from an `IconButton`. The value is written by the platform's
 * `android.icu.text.NumberFormat` in [format], in tabular figures; a compact
 * value is read in full. The card is one TalkBack element: label, value,
 * change, comparison and hint; the help button and Try again are their own
 * elements. While [loading], a placeholder replaces the value and TalkBack
 * reads "Loading"; an [error] replaces the value, the change and the hint.
 * A missing value is a dash read as "No data".
 *
 * @param label what the number measures, for example "Revenue".
 * @param value the number, or `null` when there is none.
 * @param modifier the modifier of the card.
 * @param format a number, a percentage (a ratio: 0.12 is 12%) or an amount of money.
 * @param compact writes 12K or €3.4M; TalkBack reads the full value.
 * @param locale the locale to write in. `null` uses the first locale of the configuration.
 * @param unit written smaller and muted after the value, for example "ms".
 * @param change the change against an earlier period, drawn by `MonarchDeltaIndicator`.
 * @param tone colours the value for a threshold: `Success`, `Warning`, `Danger` or `Info`. Say it in [hint] too:
 * colour is not read.
 * @param variant a card with its own surface, or a plain figure inside another surface.
 * @param size `Sm` for dense rows, `Md`, `Lg` for a hero figure.
 * @param highlighted draws the card in the accent fill and border, to single it out.
 * @param loading shows a placeholder for the value.
 * @param caption the period, for example "vs last month"; it becomes the change's comparison when the change
 * has none, and is shown alone otherwise.
 * @param hint a line under the value, for example "38 of 52 people".
 * @param help how the metric is defined, shown in a tooltip from a help button next to the label.
 * @param error replaces the value with this message.
 * @param onRetry with [error], adds Try again (`MonarchInlineErrorRetry`).
 * @param onClick makes the whole card a button that drills down into the metric.
 * @param icon a decorative icon before the label.
 * @param helpIcon the glyph of the help button. Monarch bundles no icons; without it the button shows "?".
 * @param sparkline a trend under the value, for example a `MonarchSparkline`. Decorative: the value and change
 * carry the data.
 * @param visual a decorative picture or icon at the end of the card.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the press, focus and hover interactions of a card with [onClick].
 */
@Composable
public fun MonarchStatCard(
    label: String,
    value: Double?,
    modifier: Modifier = Modifier,
    format: MonarchMetricFormat = MonarchMetricFormat.Number,
    compact: Boolean = false,
    locale: Locale? = null,
    unit: String? = null,
    change: MonarchMetricChange? = null,
    tone: MonarchTone = MonarchTone.Neutral,
    variant: MonarchStatCardVariant = MonarchStatCardVariant.Card,
    size: MonarchStatCardSize = MonarchStatCardSize.Md,
    highlighted: Boolean = false,
    loading: Boolean = false,
    caption: String? = null,
    hint: String? = null,
    help: String? = null,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    helpIcon: (@Composable () -> Unit)? = null,
    sparkline: (@Composable () -> Unit)? = null,
    visual: (@Composable () -> Unit)? = null,
    labels: MonarchStatCardLabels = MonarchStatCardLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    MonarchStatCard(
        label = label,
        text = MonarchStatCardTokens.shown(value, format, compact, resolvedLocale),
        modifier = modifier,
        spokenText = MonarchStatCardTokens.spoken(value, format, compact, resolvedLocale),
        unit = unit,
        change = change,
        tone = tone,
        variant = variant,
        size = size,
        highlighted = highlighted,
        loading = loading,
        caption = caption,
        hint = hint,
        help = help,
        error = error,
        onRetry = onRetry,
        onClick = onClick,
        icon = icon,
        helpIcon = helpIcon,
        sparkline = sparkline,
        visual = visual,
        labels = labels,
        interactionSource = interactionSource,
    )
}

/**
 * A KPI card whose value the app has already written, such as "₹12.4L" or "3 of 5".
 *
 * @param label what the value measures.
 * @param text the value as written by the app. `null` or empty shows the dash read as "No data".
 * @param modifier the modifier of the card.
 * @param spokenText what TalkBack reads instead of [text], for example the full number of an abbreviation.
 * @param unit written smaller and muted after the value.
 * @param change the change against an earlier period, drawn by `MonarchDeltaIndicator`.
 * @param tone colours the value for a threshold.
 * @param variant a card with its own surface, or a plain figure.
 * @param size `Sm`, `Md` or `Lg`.
 * @param highlighted draws the card in the accent fill and border.
 * @param loading shows a placeholder for the value.
 * @param caption the period, used as the change's comparison or shown alone.
 * @param hint a line under the value.
 * @param help how the metric is defined, in a tooltip from a help button.
 * @param error replaces the value with this message.
 * @param onRetry with [error], adds Try again.
 * @param onClick makes the whole card a button that drills down.
 * @param icon a decorative icon before the label.
 * @param helpIcon the glyph of the help button; without it the button shows "?".
 * @param sparkline a decorative trend under the value.
 * @param visual a decorative picture or icon at the end of the card.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the interactions of a card with [onClick].
 */
@Composable
public fun MonarchStatCard(
    label: String,
    text: String?,
    modifier: Modifier = Modifier,
    spokenText: String? = null,
    unit: String? = null,
    change: MonarchMetricChange? = null,
    tone: MonarchTone = MonarchTone.Neutral,
    variant: MonarchStatCardVariant = MonarchStatCardVariant.Card,
    size: MonarchStatCardSize = MonarchStatCardSize.Md,
    highlighted: Boolean = false,
    loading: Boolean = false,
    caption: String? = null,
    hint: String? = null,
    help: String? = null,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    helpIcon: (@Composable () -> Unit)? = null,
    sparkline: (@Composable () -> Unit)? = null,
    visual: (@Composable () -> Unit)? = null,
    labels: MonarchStatCardLabels = MonarchStatCardLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val tokens = MonarchStatCardTokens
    val colors = tokens.container(variant, highlighted, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.lg
    val border = colors.border?.let { BorderStroke(tokens.BorderWidth, it) }
    val loadingLabel = monarchString(labels.loading, R.string.monarch_statcard_loading)
    val state = Modifier.semantics { if (loading) stateDescription = loadingLabel }
    val body: @Composable () -> Unit = {
        Row(modifier = Modifier.padding(tokens.padding(size, variant)), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                LabelRow(label, size, icon, help, helpIcon, labels)
                StatBody(
                    shown = text?.takeIf { it.isNotEmpty() },
                    spoken = spokenText,
                    unit = unit,
                    change = change,
                    tone = tone,
                    size = size,
                    loading = loading,
                    caption = caption,
                    hint = hint,
                    error = error,
                    onRetry = onRetry,
                    sparkline = sparkline,
                    emptyLabel = labels.empty,
                )
            }
            if (visual != null) {
                Box(modifier = Modifier.padding(start = tokens.VisualGap).clearAndSetSemantics {}) { visual() }
            }
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.monarchFocusRing(shape).then(state),
            shape = shape,
            color = colors.container,
            contentColor = colors.content,
            border = border,
            interactionSource = interactionSource,
        ) { body() }
    } else {
        Surface(
            modifier = modifier.semantics(mergeDescendants = true) {}.then(state),
            shape = shape,
            color = colors.container,
            contentColor = colors.content,
            border = border,
        ) { body() }
    }
}

@Composable
private fun LabelRow(
    label: String,
    size: MonarchStatCardSize,
    icon: (@Composable () -> Unit)?,
    help: String?,
    helpIcon: (@Composable () -> Unit)?,
    labels: MonarchStatCardLabels,
) {
    val tokens = MonarchStatCardTokens
    val colors = MonarchTheme.colors
    val labelStyle = tokens.labelStyle(size, MonarchTheme.typography)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(modifier = Modifier.padding(end = tokens.IconGap).clearAndSetSemantics {}) {
                CompositionLocalProvider(LocalContentColor provides tokens.textColors(colors).muted, content = icon)
            }
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = labelStyle,
            color = tokens.textColors(colors).muted,
        )
        if (help != null) {
            var open by remember { mutableStateOf(false) }
            val helpLabel = labels.help?.invoke(label) ?: stringResource(R.string.monarch_statcard_help, label)
            // The definition opens on a tap, next to the button, and stays until dismissed.
            Box {
                MonarchIconButton(label = helpLabel, onClick = { open = true }, size = MonarchButtonSize.Sm) {
                    CompositionLocalProvider(LocalContentColor provides tokens.textColors(colors).muted) {
                        if (helpIcon != null) {
                            helpIcon()
                        } else {
                            Text(
                                text = tokens.HELP_GLYPH,
                                modifier = Modifier.clearAndSetSemantics {},
                                style = labelStyle,
                            )
                        }
                    }
                }
                MonarchPopover(
                    expanded = open,
                    onDismissRequest = { open = false },
                    label = label,
                    side = MonarchPopoverSide.Top,
                ) {
                    Text(text = label, style = MonarchTheme.typography.bodyStrong)
                    Text(text = help, style = MonarchTheme.typography.body, color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun StatBody(
    shown: String?,
    spoken: String?,
    unit: String?,
    change: MonarchMetricChange?,
    tone: MonarchTone,
    size: MonarchStatCardSize,
    loading: Boolean,
    caption: String?,
    hint: String?,
    error: String?,
    onRetry: (() -> Unit)?,
    sparkline: (@Composable () -> Unit)?,
    emptyLabel: String?,
) {
    val tokens = MonarchStatCardTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    when {
        loading -> MonarchSkeleton(
            modifier = Modifier.width(tokens.SkeletonWidth),
            shape = MonarchSkeletonShape.Text,
        )
        error != null && onRetry != null -> MonarchInlineErrorRetry(message = error, onRetry = onRetry)
        error != null -> Text(
            text = error,
            modifier = Modifier.semantics { error(error) },
            style = typography.body,
            color = tokens.textColors(colors).error,
        )
        else -> {
            Value(shown, spoken, unit, tone, size, emptyLabel)
            Footer(change, caption, size)
            if (hint != null) {
                Text(text = hint, style = typography.caption, color = tokens.textColors(colors).muted)
            }
            if (sparkline != null) {
                Box(modifier = Modifier.padding(top = tokens.SparklineGap).clearAndSetSemantics {}) { sparkline() }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Value(
    shown: String?,
    spoken: String?,
    unit: String?,
    tone: MonarchTone,
    size: MonarchStatCardSize,
    emptyLabel: String?,
) {
    val tokens = MonarchStatCardTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val empty = shown == null
    val description = if (empty) monarchString(emptyLabel, R.string.monarch_statcard_empty) else spoken
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = shown ?: monarchString(null, R.string.monarch_statcard_dash),
            modifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
            style = tokens.valueStyle(size, typography),
            color = tokens.valueColor(tone, empty, colors),
        )
        if (!empty && !unit.isNullOrEmpty()) {
            Text(text = unit, style = tokens.unitStyle(size, typography), color = tokens.textColors(colors).muted)
        }
    }
}

@Composable
private fun Footer(change: MonarchMetricChange?, caption: String?, size: MonarchStatCardSize) {
    val tokens = MonarchStatCardTokens
    when {
        change != null -> {
            val comparison = change.comparison ?: caption
            val deltaSize = tokens.deltaSize(size)
            val source = change.source
            if (source.fromValues) {
                MonarchDeltaIndicator(
                    current = source.current,
                    previous = source.previous,
                    comparison = comparison,
                    goodDirection = change.goodDirection,
                    variant = change.variant,
                    size = deltaSize,
                    flatThreshold = change.flatThreshold,
                    icon = change.icon,
                    formatValue = change.formatValue,
                )
            } else {
                MonarchDeltaIndicator(
                    value = source.value,
                    comparison = comparison,
                    goodDirection = change.goodDirection,
                    variant = change.variant,
                    size = deltaSize,
                    flatThreshold = change.flatThreshold,
                    icon = change.icon,
                    formatValue = change.formatValue,
                )
            }
        }
        caption != null -> Text(
            text = caption,
            style = MonarchTheme.typography.caption,
            color = tokens.textColors(MonarchTheme.colors).muted,
        )
    }
}
