package dev.monarchui.components.intelligence.aiconfidencemeter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * How sure the AI is about a value, as low, medium or high, in a compact badge or a labelled bar.
 *
 * Built by Monarch: the badge is a capsule like `MonarchBadge` in the colours
 * of the band; the bar is a Material 3 `LinearProgressIndicator` with a
 * label. Each band has its own words ("Needs check", "Medium confidence",
 * "High confidence") as well as its colour, and an optional icon. The meter
 * is one TalkBack element read as "Needs check, 42% confidence"; the bar is
 * also a progress bar named "Confidence". The explanation is shown under the
 * bar; for the badge it is only read by TalkBack, because touch has no
 * tooltip.
 *
 * @param value the confidence from 0 to 1. Values outside the range are clamped; not-a-number counts as 0.
 * @param modifier the modifier of the meter.
 * @param variant a compact badge or a labelled bar.
 * @param size the size.
 * @param explanation why the model is or is not confident. Shown under the bar; read after the value for the
 * badge.
 * @param lowThreshold the edge under which confidence is low.
 * @param highThreshold the edge from which confidence is high.
 * @param showValue whether the percentage is shown. `null` shows it for the bar only.
 * @param icon an icon before the band label, for example a warning mark for `Low`. Monarch bundles no icons;
 * the label always says the band in words. It takes the confidence colour as its content colour.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiConfidenceMeter(
    value: Float,
    modifier: Modifier = Modifier,
    variant: MonarchAiConfidenceMeterVariant = MonarchAiConfidenceMeterVariant.Badge,
    size: MonarchAiConfidenceMeterSize = MonarchAiConfidenceMeterSize.Md,
    explanation: String? = null,
    lowThreshold: Float = MonarchAiConfidenceLevel.DEFAULT_LOW_THRESHOLD,
    highThreshold: Float = MonarchAiConfidenceLevel.DEFAULT_HIGH_THRESHOLD,
    showValue: Boolean? = null,
    icon: (@Composable (MonarchAiConfidenceLevel) -> Unit)? = null,
    labels: MonarchAiConfidenceMeterLabels = MonarchAiConfidenceMeterLabels(),
) {
    val clamped = MonarchAiConfidenceLevel.clamp(value)
    val icon = icon ?: MonarchDefaultIcons.confidence
    val level = MonarchAiConfidenceLevel.of(clamped, lowThreshold, highThreshold)
    val label = levelLabel(level, labels)
    val percent = MonarchAiConfidenceMeterTokens.percent(clamped, LocalConfiguration.current.locales[0])
    val valueText = labels.valueText?.invoke(label, percent)
        ?: stringResource(R.string.monarch_aiconfidencemeter_value, label, percent)
    val showsValue = MonarchAiConfidenceMeterTokens.showsValue(showValue, variant)
    val meter = MeterContent(level, label, percent, valueText, showsValue)
    when (variant) {
        MonarchAiConfidenceMeterVariant.Badge -> ConfidenceBadge(meter, modifier, size, explanation, icon)
        MonarchAiConfidenceMeterVariant.Bar -> ConfidenceBar(
            meter = meter,
            fraction = clamped,
            modifier = modifier,
            size = size,
            explanation = explanation,
            name = monarchString(labels.meter, R.string.monarch_aiconfidencemeter_meter),
            icon = icon,
        )
    }
}

/** What both variants show and read. */
private class MeterContent(
    val level: MonarchAiConfidenceLevel,
    val label: String,
    val percent: String,
    val valueText: String,
    val showsValue: Boolean,
)

@Composable
private fun levelLabel(level: MonarchAiConfidenceLevel, labels: MonarchAiConfidenceMeterLabels): String = when (level) {
    MonarchAiConfidenceLevel.Low -> monarchString(labels.low, R.string.monarch_aiconfidencemeter_low)
    MonarchAiConfidenceLevel.Medium -> monarchString(labels.medium, R.string.monarch_aiconfidencemeter_medium)
    MonarchAiConfidenceLevel.High -> monarchString(labels.high, R.string.monarch_aiconfidencemeter_high)
}

@Composable
private fun ConfidenceBadge(
    meter: MeterContent,
    modifier: Modifier,
    size: MonarchAiConfidenceMeterSize,
    explanation: String?,
    icon: (@Composable (MonarchAiConfidenceLevel) -> Unit)?,
) {
    val colors = MonarchAiConfidenceMeterTokens.badgeColors(meter.level, MonarchTheme.colors)
    val spoken = if (explanation.isNullOrEmpty()) {
        meter.valueText
    } else {
        stringResource(R.string.monarch_aiconfidencemeter_with_explanation, meter.valueText, explanation)
    }
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = spoken }
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .heightIn(min = MonarchAiConfidenceMeterTokens.minHeight(size))
            .padding(horizontal = MonarchAiConfidenceMeterTokens.horizontalPadding(size)),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiConfidenceMeterTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelLabel(meter, colors.content, icon)
        if (meter.showsValue) {
            Text(text = meter.percent, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ConfidenceBar(
    meter: MeterContent,
    fraction: Float,
    modifier: Modifier,
    size: MonarchAiConfidenceMeterSize,
    explanation: String?,
    name: String,
    icon: (@Composable (MonarchAiConfidenceLevel) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val foreground = MonarchAiConfidenceMeterTokens.foreground(meter.level, colors)
    val shown by animateFloatAsState(fraction, MonarchTheme.motion.spec(), label = "confidence")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAiConfidenceMeterTokens.BarGap)) {
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = name
                stateDescription = meter.valueText
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
            verticalArrangement = Arrangement.spacedBy(MonarchAiConfidenceMeterTokens.BarGap),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                LevelLabel(meter, foreground, icon, barSize = size)
                if (meter.showsValue) {
                    Text(text = meter.percent, style = barTextStyle(size), color = colors.textMuted)
                }
            }
            LinearProgressIndicator(
                progress = { shown },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MonarchAiConfidenceMeterTokens.trackHeight(size)),
                color = foreground,
                trackColor = MonarchAiConfidenceMeterTokens.track(colors),
                strokeCap = StrokeCap.Round,
                gapSize = MonarchTheme.spacing.space0,
                drawStopIndicator = {},
            )
        }
        if (!explanation.isNullOrEmpty()) {
            Text(text = explanation, style = barTextStyle(size), color = colors.textMuted)
        }
    }
}

@Composable
private fun barTextStyle(size: MonarchAiConfidenceMeterSize) = when (size) {
    MonarchAiConfidenceMeterSize.Sm -> MonarchTheme.typography.caption
    MonarchAiConfidenceMeterSize.Md -> MonarchTheme.typography.label
}

@Composable
private fun LevelLabel(
    meter: MeterContent,
    color: Color,
    icon: (@Composable (MonarchAiConfidenceLevel) -> Unit)?,
    barSize: MonarchAiConfidenceMeterSize? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiConfidenceMeterTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides color) {
                Box(Modifier.size(MonarchAiConfidenceMeterTokens.IconSize), contentAlignment = Alignment.Center) {
                    icon(meter.level)
                }
            }
        }
        Text(
            text = meter.label,
            style = if (barSize == null) MonarchTheme.typography.caption else barTextStyle(barSize),
            fontWeight = FontWeight(MonarchAiConfidenceMeterTokens.FONT_WEIGHT),
            color = color,
        )
    }
}
