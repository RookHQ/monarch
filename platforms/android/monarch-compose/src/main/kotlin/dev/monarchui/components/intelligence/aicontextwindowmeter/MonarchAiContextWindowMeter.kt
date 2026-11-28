package dev.monarchui.components.intelligence.aicontextwindowmeter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBar
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBarLegendContent
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBarSize
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUsage
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * How full the model's context window is: a bar of what fills it with a legend, a notice as it nears the limit,
 * and an optional "Summarize earlier messages" button.
 *
 * Without a [breakdown] the bar is a Material 3 `LinearProgressIndicator` in the accent colour, turning warning
 * and then danger as the window fills. With one it is a `MonarchStackedProportionBar` of the parts, with a legend
 * of their token counts and "Other" for what no part covers. The state is always written: the summary gives the
 * count and the share, and a `MonarchInlineNotice` names the warning or the full window. When the state changes
 * while the meter is on screen it is announced once through a polite live region; the state on first appearance
 * is not announced.
 *
 * The header and bar are one TalkBack element read as "Context window" and the summary, with the range of a
 * progress bar. The [compact] meter, for a toolbar, is a small bar and the share as one button that opens the
 * full meter in a `MonarchPopover`.
 *
 * @param used the tokens in the context window now. A negative count counts as zero.
 * @param limit the model's context window, in tokens. Zero or less is always ok and shows 0%.
 * @param modifier the modifier of the meter.
 * @param breakdown what the tokens are made of, drawn as coloured segments with a legend.
 * @param warningAt the share of the limit at which the meter warns.
 * @param approximate the count is an estimate: the summary reads "About …".
 * @param summarizing a summary is being made: the button shows its progress indicator and ignores taps.
 * @param compact shows a small bar and the share as one button that opens the details in a popover.
 * @param onSummarize adds "Summarize earlier messages", which calls this.
 * @param labels replaces any of the built-in strings.
 */
@Composable
public fun MonarchAiContextWindowMeter(
    used: Long,
    limit: Long,
    modifier: Modifier = Modifier,
    breakdown: List<MonarchAiContextWindowPart> = emptyList(),
    warningAt: Double = 0.8,
    approximate: Boolean = false,
    summarizing: Boolean = false,
    compact: Boolean = false,
    onSummarize: (() -> Unit)? = null,
    labels: MonarchAiContextWindowMeterLabels = MonarchAiContextWindowMeterLabels(),
) {
    val state = MonarchAiContextWindowState.of(used, limit, warningAt)
    val summary = summaryText(used, limit, approximate, labels)
    if (compact) {
        CompactMeter(
            used, limit, breakdown, warningAt, approximate, summarizing, onSummarize, labels, state, summary, modifier,
        )
    } else {
        FullMeter(used, limit, breakdown, summarizing, onSummarize, labels, state, summary, modifier)
    }
}

/**
 * The context window meter from the usage the latest reply reported: the input and output tokens of that turn
 * are what the window holds.
 *
 * @param usage the usage of the latest message of the conversation.
 * @param limit the model's context window, in tokens.
 * @param modifier the modifier of the meter.
 * @param breakdown what the tokens are made of.
 * @param warningAt the share of the limit at which the meter warns.
 * @param approximate the count is an estimate.
 * @param summarizing a summary is being made.
 * @param compact shows the small form that opens the details in a popover.
 * @param onSummarize adds "Summarize earlier messages".
 * @param labels replaces any of the built-in strings.
 */
@Composable
public fun MonarchAiContextWindowMeter(
    usage: MonarchAiUsage,
    limit: Long,
    modifier: Modifier = Modifier,
    breakdown: List<MonarchAiContextWindowPart> = emptyList(),
    warningAt: Double = 0.8,
    approximate: Boolean = false,
    summarizing: Boolean = false,
    compact: Boolean = false,
    onSummarize: (() -> Unit)? = null,
    labels: MonarchAiContextWindowMeterLabels = MonarchAiContextWindowMeterLabels(),
) {
    MonarchAiContextWindowMeter(
        used = usage.inputTokens.coerceAtLeast(0L) + usage.outputTokens.coerceAtLeast(0L),
        limit = limit,
        modifier = modifier,
        breakdown = breakdown,
        warningAt = warningAt,
        approximate = approximate,
        summarizing = summarizing,
        compact = compact,
        onSummarize = onSummarize,
        labels = labels,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullMeter(
    used: Long,
    limit: Long,
    breakdown: List<MonarchAiContextWindowPart>,
    summarizing: Boolean,
    onSummarize: (() -> Unit)?,
    labels: MonarchAiContextWindowMeterLabels,
    state: MonarchAiContextWindowState,
    summary: String,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    val name = monarchString(labels.label, R.string.monarch_aicontextwindowmeter_label)
    val share = MonarchAiContextWindowMeterTokens.share(used, limit)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchAiContextWindowMeterTokens.Gap),
    ) {
        Column(
            // One element read as the name and the summary, with the range of a progress bar.
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(share, 0f..1f)
            },
            verticalArrangement = Arrangement.spacedBy(MonarchAiContextWindowMeterTokens.Gap),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(MonarchAiContextWindowMeterTokens.HeaderGap),
            ) {
                MonarchText(text = name, style = MonarchTextStyle.Label, heading = false)
                MonarchText(text = summary, style = MonarchTextStyle.Caption, color = colors.textMuted)
            }
            if (breakdown.isEmpty()) SingleBar(share, state, Modifier.fillMaxWidth())
        }
        if (breakdown.isNotEmpty()) {
            val other = monarchString(labels.other, R.string.monarch_aicontextwindowmeter_other)
            val number = rememberIntegerFormat()
            val tokensLabel = labels.tokens
            val tokensFormat = stringResource(R.string.monarch_aicontextwindowmeter_tokens)
            val locale = LocalConfiguration.current.locales[0]
            MonarchStackedProportionBar(
                label = monarchString(labels.legend, R.string.monarch_aicontextwindowmeter_legend),
                segments = MonarchAiContextWindowMeterTokens.segments(used, breakdown, other),
                total = MonarchAiContextWindowMeterTokens.total(used, limit, breakdown),
                legendContent = MonarchStackedProportionBarLegendContent.Value,
                size = MonarchStackedProportionBarSize.Md,
                formatValue = { value ->
                    val formatted = number.format(value.toLong())
                    tokensLabel?.invoke(formatted) ?: String.format(locale, tokensFormat, formatted)
                },
            )
        }
        Notice(state, labels)
        if (onSummarize != null) {
            MonarchButton(
                text = monarchString(labels.summarize, R.string.monarch_aicontextwindowmeter_summarize),
                onClick = onSummarize,
                variant = if (state == MonarchAiContextWindowState.Ok) {
                    MonarchButtonVariant.Ghost
                } else {
                    MonarchButtonVariant.Secondary
                },
                size = MonarchButtonSize.Sm,
                loading = summarizing,
            )
        }
    }
}

@Composable
private fun SingleBar(share: Float, state: MonarchAiContextWindowState, modifier: Modifier) {
    val colors = MonarchTheme.colors
    LinearProgressIndicator(
        progress = { share },
        // The parent element carries the name, the summary and the range; the indicator adds no element of its own.
        modifier = modifier.height(MonarchAiContextWindowMeterTokens.BarHeight).clearAndSetSemantics {},
        color = MonarchAiContextWindowMeterTokens.fill(state, colors),
        trackColor = MonarchAiContextWindowMeterTokens.track(colors),
        gapSize = MonarchTheme.spacing.space0,
        drawStopIndicator = {},
    )
}

@Composable
private fun Notice(state: MonarchAiContextWindowState, labels: MonarchAiContextWindowMeterLabels) {
    val message = when (state) {
        MonarchAiContextWindowState.Ok -> null
        MonarchAiContextWindowState.Warning ->
            monarchString(labels.warning, R.string.monarch_aicontextwindowmeter_warning)
        MonarchAiContextWindowState.Full -> monarchString(labels.full, R.string.monarch_aicontextwindowmeter_full)
    }
    val tone = MonarchAiContextWindowMeterTokens.noticeTone(state)
    if (message != null && tone != null) MonarchInlineNotice(message = message, tone = tone)
    // Always present, so a change of state is announced once as a change of this live region.
    val first = remember { state }
    var changed by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if (state != first) changed = true }
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (changed && message != null) contentDescription = message
        },
    )
}

@Composable
private fun CompactMeter(
    used: Long,
    limit: Long,
    breakdown: List<MonarchAiContextWindowPart>,
    warningAt: Double,
    approximate: Boolean,
    summarizing: Boolean,
    onSummarize: (() -> Unit)?,
    labels: MonarchAiContextWindowMeterLabels,
    state: MonarchAiContextWindowState,
    summary: String,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    val name = monarchString(labels.label, R.string.monarch_aicontextwindowmeter_label)
    val action = monarchString(labels.showDetails, R.string.monarch_aicontextwindowmeter_show_details)
    val share = MonarchAiContextWindowMeterTokens.share(used, limit)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .monarchMinimumTouchTarget()
                .monarchFocusRing(MonarchTheme.shapes.sm)
                .clickable(role = Role.Button, onClickLabel = action) { open = true }
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = name
                    stateDescription = summary
                    progressBarRangeInfo = ProgressBarRangeInfo(share, 0f..1f)
                    onClick(label = action) {
                        open = true
                        true
                    }
                }
                .padding(MonarchAiContextWindowMeterTokens.CompactPadding),
            horizontalArrangement = Arrangement.spacedBy(MonarchAiContextWindowMeterTokens.CompactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleBar(share, state, Modifier.width(MonarchAiContextWindowMeterTokens.CompactBarWidth))
            MonarchText(
                text = rememberPercentFormat().format(share.toDouble()),
                style = MonarchTextStyle.Caption,
                color = MonarchAiContextWindowMeterTokens.compactText(state, colors),
            )
        }
        MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = name) {
            MonarchAiContextWindowMeter(
                used = used,
                limit = limit,
                breakdown = breakdown,
                warningAt = warningAt,
                approximate = approximate,
                summarizing = summarizing,
                onSummarize = onSummarize,
                labels = labels,
            )
        }
    }
}

@Composable
private fun summaryText(
    used: Long,
    limit: Long,
    approximate: Boolean,
    labels: MonarchAiContextWindowMeterLabels,
): String {
    val number = rememberIntegerFormat()
    val usedText = number.format(MonarchAiContextWindowMeterTokens.used(used))
    val limitText = number.format(limit.coerceAtLeast(0L))
    val percent = rememberPercentFormat().format(MonarchAiContextWindowMeterTokens.share(used, limit).toDouble())
    val exact = labels.amount?.invoke(usedText, limitText, percent)
        ?: stringResource(R.string.monarch_aicontextwindowmeter_amount, usedText, limitText, percent)
    if (!approximate) return exact
    return labels.approximate?.invoke(exact) ?: stringResource(R.string.monarch_aicontextwindowmeter_approximate, exact)
}

@Composable
private fun rememberIntegerFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}

@Composable
private fun rememberPercentFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getPercentInstance(locale) }
}
