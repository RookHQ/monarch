package com.rookhq.monarch.components.intelligence.aiscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRing
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingColor
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingLabels
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/**
 * A 0 to 100 score produced by a model, as a ring or a bar, with its band,
 * the rationale and the factors that raised or lowered it.
 *
 * Built from Monarch parts: the ring is `MonarchScoreRing` in the band's
 * tone, the bar is `MonarchProgress`, the band is a `MonarchBadge`, the AI
 * mark is `MonarchAiGeneratedBadge` and the band legend folds in a
 * `MonarchCollapsible`. Everything drawn is also written: the number, the
 * band name and each factor's signed impact.
 *
 * The ring is one TalkBack element named by the label with the value "72 out
 * of 100"; the bar is one element with the value "72 out of 100, High". Each
 * factor is one element ("Opened three emails, Raised the score by 18") and
 * each legend row is one element that says which band is this score. The
 * label and the factors heading are headings.
 *
 * @param value the score from 0 to 100; values outside are limited. `null` while there is no score.
 * @param modifier the modifier of the component.
 * @param variant a ring with the number in its centre, or a bar with the number above it.
 * @param size the size of the ring.
 * @param label what is scored, for example "Lead score". "Score" when `null`.
 * @param bands named ranges with a minimum, a label and a tone. Low (0, danger), Medium (40, warning) and High
 * (70, success) by default.
 * @param rationale why the model gave this score, in plain words.
 * @param factors the inputs that moved the score.
 * @param showBands whether the band legend is offered, folded under "How to read this score".
 * @param aiIcon an icon for the AI mark, for example a `MonarchIcon` of a bot. Monarch bundles no icons.
 * @param bandIcon an icon for the band badge, given the band's tone. Decorative: the band name is written.
 * @param expandIcon the chevron of the band legend, passed to `MonarchCollapsible`.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiScore(
    value: Double?,
    modifier: Modifier = Modifier,
    variant: MonarchAiScoreVariant = MonarchAiScoreVariant.Ring,
    size: MonarchAiScoreSize = MonarchAiScoreSize.Md,
    label: String? = null,
    bands: List<MonarchAiScoreBand> = MonarchAiScoreBand.defaults(),
    rationale: String? = null,
    factors: List<MonarchAiScoreFactor> = emptyList(),
    showBands: Boolean = true,
    aiIcon: (@Composable () -> Unit)? = null,
    bandIcon: (@Composable (MonarchTone) -> Unit)? = null,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiScoreLabels = MonarchAiScoreLabels(),
) {
    val tokens = MonarchAiScoreTokens
    val locale = LocalConfiguration.current.locales[0]
    val score = tokens.clamp(value)
    val band = score?.let { tokens.band(it, bands) }
    val name = label ?: monarchString(labels.label, R.string.monarch_aiscore_label)
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        Header(name, aiIcon, labels)
        Display(score, band, variant, size, name, locale, bandIcon, labels)
        if (!rationale.isNullOrBlank()) {
            Text(
                text = rationale,
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textDefault,
            )
        }
        if (factors.isNotEmpty()) Factors(factors, locale, labels)
        if (showBands && bands.isNotEmpty()) Legend(bands, band, locale, expandIcon, labels)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(name: String, aiIcon: (@Composable () -> Unit)?, labels: MonarchAiScoreLabels) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiScoreTokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiScoreTokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = MonarchTheme.colors.textDefault,
        )
        MonarchAiGeneratedBadge(
            size = MonarchAiGeneratedBadgeSize.Sm,
            icon = aiIcon,
            label = monarchString(labels.aiGenerated, R.string.monarch_aiscore_ai_generated),
        )
    }
}

/** The texts of the score: the number, the maximum and the spoken value. */
private class ScoreTexts(val number: String?, val max: String, val value: String?, val unavailable: String)

@Suppress("LongParameterList")
@Composable
private fun Display(
    score: Double?,
    band: AiScoreBandRange?,
    variant: MonarchAiScoreVariant,
    size: MonarchAiScoreSize,
    name: String,
    locale: Locale,
    bandIcon: (@Composable (MonarchTone) -> Unit)?,
    labels: MonarchAiScoreLabels,
) {
    val tokens = MonarchAiScoreTokens
    val number = score?.let { tokens.formatWhole(it, locale) }
    val maxText = tokens.formatWhole(tokens.MAX, locale)
    val texts = ScoreTexts(
        number = number,
        max = maxText,
        value = number?.let {
            labels.valueText?.invoke(it, maxText) ?: stringResource(R.string.monarch_aiscore_value, it, maxText)
        },
        unavailable = monarchString(labels.unavailable, R.string.monarch_aiscore_unavailable),
    )
    val badge: @Composable () -> Unit = {
        if (band != null) {
            MonarchBadge(
                text = band.band.label,
                tone = band.band.tone,
                leadingIcon = bandIcon?.let { icon -> { icon(band.band.tone) } },
            )
        }
    }
    when (variant) {
        MonarchAiScoreVariant.Ring -> RingDisplay(score, band, size, name, locale, texts, badge)
        MonarchAiScoreVariant.Bar -> BarDisplay(score, band, name, texts, badge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun RingDisplay(
    score: Double?,
    band: AiScoreBandRange?,
    size: MonarchAiScoreSize,
    name: String,
    locale: Locale,
    texts: ScoreTexts,
    badge: @Composable () -> Unit,
) {
    val tokens = MonarchAiScoreTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.DisplayGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchScoreRing(
            value = score,
            color = MonarchScoreRingColor.Tone(band?.band?.tone ?: MonarchTone.Neutral),
            size = tokens.ringSize(size),
            format = { tokens.formatWhole(it, locale) },
            labels = MonarchScoreRingLabels(
                name = name,
                valueText = { _, _ -> texts.value.orEmpty() },
                noValue = texts.unavailable,
            ),
        )
        badge()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarDisplay(
    score: Double?,
    band: AiScoreBandRange?,
    name: String,
    texts: ScoreTexts,
    badge: @Composable () -> Unit,
) {
    val tokens = MonarchAiScoreTokens
    val spoken = when {
        texts.value == null -> texts.unavailable
        band == null -> texts.value
        else -> stringResource(R.string.monarch_aiscore_value_band, texts.value, band.band.label)
    }
    // One element: the number, the maximum and the band are read in the value.
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = name
            stateDescription = spoken
            if (score != null) {
                progressBarRangeInfo = ProgressBarRangeInfo(score.toFloat(), 0f..tokens.MAX.toFloat())
            }
        },
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (texts.number != null) {
                Text(
                    text = texts.number,
                    style = MonarchTheme.typography.title,
                    fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
                    color = MonarchTheme.colors.textDefault,
                )
                Text(
                    text = stringResource(R.string.monarch_aiscore_of_max, texts.max),
                    style = MonarchTheme.typography.caption,
                    color = MonarchTheme.colors.textMuted,
                )
            } else {
                Text(
                    text = texts.unavailable,
                    style = MonarchTheme.typography.body,
                    color = MonarchTheme.colors.textMuted,
                )
            }
            badge()
        }
        MonarchProgress(
            modifier = Modifier.fillMaxWidth(),
            progress = ((score ?: 0.0) / tokens.MAX).toFloat(),
            tone = tokens.barTone(band?.band?.tone),
        )
    }
}

@Composable
private fun Factors(factors: List<MonarchAiScoreFactor>, locale: Locale, labels: MonarchAiScoreLabels) {
    val tokens = MonarchAiScoreTokens
    val colors = MonarchTheme.colors
    val maxImpact = tokens.maxImpact(factors)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
        Text(
            text = monarchString(labels.factors, R.string.monarch_aiscore_factors),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = colors.textDefault,
        )
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        ) {
            Text(
                text = monarchString(labels.factorColumn, R.string.monarch_aiscore_factor_column),
                modifier = Modifier.weight(1f),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
            Text(
                text = monarchString(labels.impactColumn, R.string.monarch_aiscore_impact_column),
                modifier = Modifier.width(tokens.ImpactWidth),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
        }
        factors.forEach { factor ->
            val amount = tokens.formatImpact(factor.impact, locale)
            val raised = factor.impact > 0
            val spokenImpact = when {
                factor.impact == 0.0 || !factor.impact.isFinite() ->
                    monarchString(labels.noImpact, R.string.monarch_aiscore_no_impact)
                else -> labels.impact?.invoke(amount, raised) ?: stringResource(
                    if (raised) R.string.monarch_aiscore_raised else R.string.monarch_aiscore_lowered,
                    amount,
                )
            }
            val signed = when {
                factor.impact > 0 -> stringResource(R.string.monarch_aiscore_signed_up, amount)
                factor.impact < 0 -> stringResource(R.string.monarch_aiscore_signed_down, amount)
                else -> amount
            }
            val impactColor = tokens.impactColor(factor.impact, colors)
            val fraction = tokens.impactFraction(factor.impact, maxImpact)
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = listOfNotNull(factor.label, spokenImpact, factor.detail).joinToString(", ")
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
                    Text(text = factor.label, style = MonarchTheme.typography.body, color = colors.textDefault)
                    if (!factor.detail.isNullOrBlank()) {
                        Text(text = factor.detail, style = MonarchTheme.typography.caption, color = colors.textMuted)
                    }
                }
                Column(
                    modifier = Modifier.width(tokens.ImpactWidth),
                    verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
                ) {
                    ImpactBar(factor.impact, fraction, impactColor)
                    CompositionLocalProvider(LocalContentColor provides impactColor) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when {
                                factor.impact > 0 -> MonarchDefaultIcons.ArrowUp()
                                factor.impact < 0 -> MonarchDefaultIcons.ArrowDown()
                            }
                            Text(text = signed, style = MonarchTheme.typography.caption, color = impactColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(
    bands: List<MonarchAiScoreBand>,
    current: AiScoreBandRange?,
    locale: Locale,
    expandIcon: (@Composable () -> Unit)?,
    labels: MonarchAiScoreLabels,
) {
    val tokens = MonarchAiScoreTokens
    val colors = MonarchTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    val mark = monarchString(labels.currentBand, R.string.monarch_aiscore_current_band)
    MonarchCollapsible(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        label = monarchString(labels.bands, R.string.monarch_aiscore_bands),
        trailingIcon = expandIcon,
    ) {
        tokens.ranges(bands).forEach { range ->
            val isCurrent = current?.band === range.band
            val bandText = if (isCurrent) {
                stringResource(R.string.monarch_aiscore_band_current, range.band.label, mark)
            } else {
                range.band.label
            }
            val rangeText = stringResource(
                R.string.monarch_aiscore_range,
                tokens.formatWhole(range.band.minimum.coerceIn(0.0, tokens.MAX), locale),
                tokens.formatWhole(range.max, locale),
            )
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = "$bandText, $rangeText"
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(tokens.SwatchSize)
                        .background(tokens.swatchColor(range.band.tone, colors), MonarchShapeDefaults.Mark),
                )
                Text(
                    text = bandText,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.body,
                    fontWeight = if (isCurrent) FontWeight(MonarchTokens.FontWeight.semibold) else null,
                    color = colors.textDefault,
                )
                Text(text = rangeText, style = MonarchTheme.typography.body, color = colors.textMuted)
            }
        }
    }
}

/** The bar of a factor grows from the middle of its track: right when it raised the score, left when it lowered it. */
@Composable
private fun ImpactBar(impact: Double, fraction: Float, color: Color) {
    val tokens = MonarchAiScoreTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(tokens.ImpactBarHeight)
            .background(MonarchTheme.colors.bgMuted, MonarchShapeDefaults.Mark),
    ) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
            if (impact < 0) ImpactFill(fraction, color)
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            if (impact > 0) ImpactFill(fraction, color)
        }
    }
}

@Composable
private fun ImpactFill(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color, MonarchShapeDefaults.Mark))
}
