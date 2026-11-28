package dev.monarchui.components.charts.deltaindicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A change between two periods: a signed value, an optional arrow and an
 * optional comparison, such as "+12% vs last month".
 *
 * Built by Monarch: Material 3 has no equivalent. The colour follows
 * [goodDirection], so a fall in costs is green, and the meaning is also in
 * the sign and in the sentence TalkBack reads ("up 12% vs last month,
 * better"), so nothing depends on colour. The indicator is one element and is
 * not interactive.
 *
 * @param value the change. With the default writer it is a ratio: 0.12 is 12%. `null` or a value that is
 * not finite means there is nothing to compare with.
 * @param modifier the modifier of the indicator.
 * @param comparison the comparison period after the change, such as "vs last month". Shown muted and spoken.
 * @param goodDirection which direction is an improvement.
 * @param variant plain text or a tinted pill.
 * @param size the text size.
 * @param flatThreshold changes whose size is at or below this count as no change.
 * @param icon draws the arrow for a direction, for example a `MonarchIcon` with a `null` content
 * description. It is decoration, tinted with the tone colour and hidden from TalkBack. Monarch bundles no
 * icons, so without it the sign and the words carry the direction.
 * @param formatValue writes the size of the change, without a sign. `null` writes a percentage with at most
 * one fraction digit in the current locale.
 * @param flatLabel replaces the built-in text "No change" (`monarch_deltaindicator_flat`).
 * @param noDataLabel replaces the built-in text "No earlier data" (`monarch_deltaindicator_no_data`).
 * @param spokenLabel replaces the whole sentence TalkBack reads.
 */
@Composable
public fun MonarchDeltaIndicator(
    value: Double?,
    modifier: Modifier = Modifier,
    comparison: String? = null,
    goodDirection: MonarchDeltaIndicatorGoodDirection = MonarchDeltaIndicatorGoodDirection.Up,
    variant: MonarchDeltaIndicatorVariant = MonarchDeltaIndicatorVariant.Plain,
    size: MonarchDeltaIndicatorSize = MonarchDeltaIndicatorSize.Sm,
    flatThreshold: Double = 0.0,
    icon: (@Composable (direction: MonarchDeltaIndicatorDirection) -> Unit)? = MonarchDefaultIcons.Delta,
    formatValue: ((Double) -> String)? = null,
    flatLabel: String? = null,
    noDataLabel: String? = null,
    spokenLabel: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val write = formatValue ?: remember(locale) { MonarchDeltaIndicatorReading.percent(locale) }
    val reading = MonarchDeltaIndicatorReading.of(value, goodDirection, flatThreshold, write)
    val flat = monarchString(flatLabel, R.string.monarch_deltaindicator_flat)
    val noData = monarchString(noDataLabel, R.string.monarch_deltaindicator_no_data)
    val sentence = spokenLabel ?: spokenSentence(reading, comparison, flat, noData)
    val colors = MonarchTheme.colors
    val style = MonarchDeltaIndicatorTokens.textStyle(size, MonarchTheme.typography)
        .copy(fontFeatureSettings = MonarchDeltaIndicatorTokens.TABULAR_FIGURES)
    val direction = reading.direction
    // A flow, so a narrow card moves the comparison to its own line instead of breaking it letter by letter.
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = sentence },
        horizontalArrangement = Arrangement.spacedBy(MonarchDeltaIndicatorTokens.Gap),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (direction == null) {
            Text(text = noData, style = style, color = MonarchDeltaIndicatorTokens.mutedColor(colors))
            return@FlowRow
        }
        val change = if (direction == MonarchDeltaIndicatorDirection.Flat) flat else reading.signedText
        Change(change, direction, reading.tone, variant, style, icon)
        if (comparison != null) {
            Text(
                text = comparison,
                style = style,
                color = MonarchDeltaIndicatorTokens.mutedColor(colors),
            )
        }
    }
}

/**
 * A change worked out from two periods: `(current − previous) / |previous|`.
 *
 * A previous value of zero has no baseline and shows "No earlier data". For
 * an absolute difference, subtract and use the overload that takes the value.
 *
 * @param current this period's value.
 * @param previous the previous period's value.
 * @param modifier the modifier of the indicator.
 * @param comparison the comparison period after the change, such as "vs last week".
 * @param goodDirection which direction is an improvement.
 * @param variant plain text or a tinted pill.
 * @param size the text size.
 * @param flatThreshold changes whose size is at or below this count as no change.
 * @param icon draws the arrow for a direction; decoration, hidden from TalkBack.
 * @param formatValue writes the size of the change, without a sign. `null` writes a percentage.
 * @param flatLabel replaces the built-in text "No change".
 * @param noDataLabel replaces the built-in text "No earlier data".
 * @param spokenLabel replaces the whole sentence TalkBack reads.
 */
@Composable
public fun MonarchDeltaIndicator(
    current: Double?,
    previous: Double?,
    modifier: Modifier = Modifier,
    comparison: String? = null,
    goodDirection: MonarchDeltaIndicatorGoodDirection = MonarchDeltaIndicatorGoodDirection.Up,
    variant: MonarchDeltaIndicatorVariant = MonarchDeltaIndicatorVariant.Plain,
    size: MonarchDeltaIndicatorSize = MonarchDeltaIndicatorSize.Sm,
    flatThreshold: Double = 0.0,
    icon: (@Composable (direction: MonarchDeltaIndicatorDirection) -> Unit)? = MonarchDefaultIcons.Delta,
    formatValue: ((Double) -> String)? = null,
    flatLabel: String? = null,
    noDataLabel: String? = null,
    spokenLabel: String? = null,
) {
    MonarchDeltaIndicator(
        value = MonarchDeltaIndicatorReading.change(current, previous),
        modifier = modifier,
        comparison = comparison,
        goodDirection = goodDirection,
        variant = variant,
        size = size,
        flatThreshold = flatThreshold,
        icon = icon,
        formatValue = formatValue,
        flatLabel = flatLabel,
        noDataLabel = noDataLabel,
        spokenLabel = spokenLabel,
    )
}

@Composable
private fun Change(
    text: String,
    direction: MonarchDeltaIndicatorDirection,
    tone: MonarchTone,
    variant: MonarchDeltaIndicatorVariant,
    style: TextStyle,
    icon: (@Composable (MonarchDeltaIndicatorDirection) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val pill = variant == MonarchDeltaIndicatorVariant.Subtle
    val surface = MonarchDeltaIndicatorTokens.pillColors(tone, colors)
    val content: Color = if (pill) surface.content else MonarchDeltaIndicatorTokens.plainColor(tone, colors)
    val frame = if (pill) {
        Modifier
            .monarchSurface(surface, MonarchShapeDefaults.Mark)
            .padding(
                horizontal = MonarchDeltaIndicatorTokens.PillHorizontalPadding,
                vertical = MonarchDeltaIndicatorTokens.PillVerticalPadding,
            )
    } else {
        Modifier
    }
    Row(
        modifier = frame,
        horizontalArrangement = Arrangement.spacedBy(MonarchDeltaIndicatorTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            icon?.invoke(direction)
            Text(text = text, style = style, color = content)
        }
    }
}

@Composable
private fun spokenSentence(
    reading: MonarchDeltaIndicatorReading,
    comparison: String?,
    flat: String,
    noData: String,
): String {
    val tail = comparison?.let { " $it" }.orEmpty()
    val change = when (reading.direction) {
        null -> return noData
        MonarchDeltaIndicatorDirection.Flat -> return flat + tail
        MonarchDeltaIndicatorDirection.Up -> stringResource(R.string.monarch_deltaindicator_up, reading.magnitude)
        MonarchDeltaIndicatorDirection.Down -> stringResource(R.string.monarch_deltaindicator_down, reading.magnitude)
    } + tail
    return when (reading.tone) {
        MonarchTone.Success -> stringResource(R.string.monarch_deltaindicator_better, change)
        MonarchTone.Danger -> stringResource(R.string.monarch_deltaindicator_worse, change)
        else -> change
    }
}
