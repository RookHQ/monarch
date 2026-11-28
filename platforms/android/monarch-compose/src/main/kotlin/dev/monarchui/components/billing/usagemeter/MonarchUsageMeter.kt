package dev.monarchui.components.billing.usagemeter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.theme.MonarchTheme
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Usage against limits: for each metered item, what was used, the limit and
 * a meter that turns to the warning colour near the limit and the danger
 * colour over it, with the level written as text.
 *
 * Built from `MonarchProgress` bars (thin, no end dot) in the level's tone; one
 * per item with a limit. Each item is one TalkBack element that reads its
 * label, its figures and its level, so nothing depends on the colour of the
 * meter. Items without a limit show the count alone.
 *
 * @param items the metered items, in display order.
 * @param modifier the modifier of the meter.
 * @param title the heading, already localised.
 * @param period the period measured, such as "This month".
 * @param resetsAtMillis when the quota resets, in epoch milliseconds (UTC). Written as a date in the current
 * locale and [timeZone].
 * @param timeZone the zone the reset date is written in. An IANA id such as `"Europe/Berlin"`; `null`, or an id the
 * device does not know, is the device's zone.
 * @param warningAt the share of the limit at which an item is near the limit.
 * @param loading shows a progress indicator and "Loading usage" instead of the items.
 * @param announceChanges makes every item a polite live region, so TalkBack announces an item whose figures or
 * level change while it is on screen. Off by default, because the meter is usually on screen from the start.
 * @param error a message shown instead of the items. It wins over [loading].
 * @param onRetry adds a "Try again" button under the error.
 * @param labels replaces any of the built-in strings.
 * @param announceThresholds announces, once for the whole meter, the items that have just reached the warning
 * share or gone over their limit ("Storage: Near the limit"), through one polite live region. Lighter than
 * [announceChanges], which reads every change of every item.
 * @param statusIcon an icon before the status line of an item near its limit (`over` is false) or over it, such as a
 * warning sign; the default is the web's: the error mark when over, the warning triangle when near.
 */
@Composable
public fun MonarchUsageMeter(
    items: List<MonarchUsageMeterItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    period: String? = null,
    resetsAtMillis: Long? = null,
    timeZone: String? = null,
    warningAt: Double = MonarchUsageMeterTokens.DEFAULT_WARNING_AT,
    loading: Boolean = false,
    announceChanges: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    labels: MonarchUsageMeterLabels = MonarchUsageMeterLabels(),
    announceThresholds: Boolean = false,
    statusIcon: (@Composable (over: Boolean) -> Unit)? = MonarchDefaultIcons.usageStatus,
) {
    val locale = LocalConfiguration.current.locales[0]
    val body = @Composable { bodyModifier: Modifier ->
        Column(modifier = bodyModifier, verticalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.ItemGap)) {
            Header(title, period, resetsAtMillis, monarchTimeZone(timeZone), locale, labels)
            when {
                error != null -> ErrorBody(error, onRetry, labels)
                loading -> LoadingBody(labels)
                else -> items.forEach { item ->
                    UsageItem(item, warningAt, announceChanges, locale, labels, statusIcon)
                }
            }
        }
    }
    if (announceThresholds) {
        // The live region sits beside the column, so it adds no gap between the items.
        Box(modifier = modifier) {
            ThresholdAnnouncer(items, warningAt, labels)
            body(Modifier)
        }
    } else {
        body(modifier)
    }
}

/** One polite announcement for the items that crossed a threshold in the last change. */
@Composable
private fun ThresholdAnnouncer(items: List<MonarchUsageMeterItem>, warningAt: Double, labels: MonarchUsageMeterLabels) {
    val announcer = rememberMonarchAnnouncer()
    val levels = remember { mutableMapOf<String, MonarchUsageLevel>() }
    val near = monarchString(labels.nearLimit, R.string.monarch_usagemeter_near_limit)
    val over = monarchString(labels.overLimit, R.string.monarch_usagemeter_over_limit)
    val resources = LocalResources.current
    LaunchedEffect(items, warningAt) {
        val crossed = MonarchUsageMeterTokens.crossings(levels, items, warningAt)
        levels.clear()
        items.forEach { levels[it.id] = MonarchUsageMeterTokens.level(it.used, it.limit, warningAt) }
        if (crossed.isNotEmpty()) {
            announcer.announce(
                crossed.joinToString(", ") { (item, level) ->
                    resources.getString(
                        R.string.monarch_usagemeter_threshold,
                        item.label,
                        if (level == MonarchUsageLevel.Over) over else near,
                    )
                },
            )
        }
    }
    MonarchAnnouncerNode(announcer)
}

@Composable
private fun Header(
    title: String?,
    period: String?,
    resetsAtMillis: Long?,
    zone: java.util.TimeZone,
    locale: Locale,
    labels: MonarchUsageMeterLabels,
) {
    val resets = resetsAtMillis?.let { millis ->
        val date = remember(millis, locale, zone) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { timeZone = zone }.format(Date(millis))
        }
        labels.resets?.invoke(date) ?: stringResource(R.string.monarch_usagemeter_resets, date)
    }
    val line = listOfNotNull(period, resets).joinToString(" · ").ifEmpty { null }
    if (title == null && line == null) return
    Column(verticalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.HeaderGap)) {
        title?.let { MonarchText(text = it, style = MonarchTextStyle.Subheading) }
        line?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted) }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun UsageItem(
    item: MonarchUsageMeterItem,
    warningAt: Double,
    announceChanges: Boolean,
    locale: Locale,
    labels: MonarchUsageMeterLabels,
    statusIcon: (@Composable (over: Boolean) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val numbers = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
    }
    val percents = remember(locale) { NumberFormat.getPercentInstance(locale) }
    val level = MonarchUsageMeterTokens.level(item.used, item.limit, warningAt)
    val limit = MonarchUsageMeterTokens.limitOf(item.limit)
    val used = numbers.format(item.used)
    val figures = if (limit != null) {
        val written = numbers.format(limit)
        labels.amount?.invoke(used, written) ?: stringResource(R.string.monarch_usagemeter_amount, used, written)
    } else {
        used
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (announceChanges) liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.LineGap),
    ) {
        MonarchText(text = item.label, style = MonarchTextStyle.Label)
        MonarchText(
            text = listOfNotNull(figures, item.unit).joinToString(" "),
            style = MonarchTextStyle.BodyStrong,
        )
        if (limit != null) {
            MonarchProgress(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                progress = MonarchUsageMeterTokens.fraction(item.used, limit),
                tone = MonarchUsageMeterTokens.meterTone(level),
                size = MonarchProgressSize.Sm,
            )
            val share = percents.format(item.used / limit)
            val percent = labels.percent?.invoke(share) ?: stringResource(R.string.monarch_usagemeter_percent, share)
            val status = when (level) {
                MonarchUsageLevel.Warning -> monarchString(labels.nearLimit, R.string.monarch_usagemeter_near_limit)
                MonarchUsageLevel.Over -> monarchString(labels.overLimit, R.string.monarch_usagemeter_over_limit)
                else -> null
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.LineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (status != null && statusIcon != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides MonarchUsageMeterTokens.statusColor(level, colors),
                    ) { statusIcon(level == MonarchUsageLevel.Over) }
                }
                Text(
                    text = status?.let { stringResource(R.string.monarch_usagemeter_status, it, percent) } ?: percent,
                    style = MonarchTheme.typography.caption,
                    color = MonarchUsageMeterTokens.statusColor(level, colors),
                )
            }
        }
        item.description?.let {
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun LoadingBody(labels: MonarchUsageMeterLabels) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.clearAndSetSemantics { },
            color = colors.accentSolid,
            trackColor = colors.bgEmphasis,
        )
        MonarchText(
            text = monarchString(labels.loading, R.string.monarch_usagemeter_loading),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun ErrorBody(error: String, onRetry: (() -> Unit)?, labels: MonarchUsageMeterLabels) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchUsageMeterTokens.LineGap)) {
        MonarchText(
            text = error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MonarchTheme.colors.dangerStrong,
        )
        onRetry?.let { retry ->
            MonarchButton(text = monarchString(labels.retry, R.string.monarch_usagemeter_retry), onClick = retry)
        }
    }
}
