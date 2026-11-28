package dev.monarchui.components.collaboration.callstatspanel

import android.icu.math.BigDecimal
import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * The data a call has used and each person's round trip, packet loss and connection grade, for a call's
 * "Connection" panel.
 *
 * Built by Monarch from `MonarchDescriptionList` rows and a `MonarchStatusBadge` per person. The panel is the
 * content, not the sheet: present it in a Material 3 `ModalBottomSheet` (or a pane) from the call controls, as in
 * the example, because the sheet's state and dismissal belong to the app. It takes plain numbers, which the app
 * reads from its WebRTC statistics, and holds no timer: pass new numbers as they arrive.
 *
 * For TalkBack the heading is a heading, the people are a group named "Connection to each person" and each person
 * is one element read as the name, the round trip, the loss and the grade ("Alex Chen, Round trip 186 ms, Loss
 * 3.4%, Unstable connection"). A number that was not measured shows a dash and is read as "Not measured yet".
 *
 * @param usage the bytes sent and received and the current upload and download rates.
 * @param peers one entry per remote person.
 * @param modifier the modifier of the panel.
 * @param lowData adds the note "Low data mode is on: audio only.".
 * @param footnote a closing note, such as how media is routed or protected. The app's own words: Monarch does not
 * know how the app connects calls.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchCallStatsPanel(
    usage: MonarchCallStatsPanelUsage,
    peers: List<MonarchCallStatsPanelPeer>,
    modifier: Modifier = Modifier,
    lowData: Boolean = false,
    footnote: String? = null,
    labels: MonarchCallStatsPanelLabels = MonarchCallStatsPanelLabels(),
) {
    val number = rememberNumber()
    val units = callUnits()
    val muted = MonarchCallStatsPanelTokens.muted(MonarchTheme.colors)
    val total = MonarchCallStatsPanelFormat.bytes(
        usage.sentBytes.coerceAtLeast(0) + usage.receivedBytes.coerceAtLeast(0), units, number,
    )
    val up = MonarchCallStatsPanelFormat.rate(usage.upBitsPerSecond, units, number)
    val down = MonarchCallStatsPanelFormat.rate(usage.downBitsPerSecond, units, number)
    val usageLine = labels.usageSummary?.invoke(total, up, down)
        ?: stringResource(R.string.monarch_callstatspanel_usage_summary, total, up, down)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchCallStatsPanelTokens.SectionGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchCallStatsPanelTokens.InnerGap)) {
            MonarchText(
                text = monarchString(labels.dataUsed, R.string.monarch_callstatspanel_data_used),
                style = MonarchTextStyle.Subheading,
                heading = true,
            )
            MonarchText(text = usageLine, style = MonarchTextStyle.Caption, color = muted)
            if (lowData) {
                MonarchText(
                    text = monarchString(labels.lowData, R.string.monarch_callstatspanel_low_data),
                    style = MonarchTextStyle.Caption,
                    color = muted,
                )
            }
        }
        if (peers.isEmpty()) {
            MonarchText(text = monarchString(labels.noPeers, R.string.monarch_callstatspanel_no_peers), color = muted)
        } else {
            val words = peerWords(labels)
            val lossFormat = rememberLossFormat()
            Column(
                modifier = Modifier.fillMaxWidth().semantics {
                    isTraversalGroup = true
                    contentDescription = words.connections
                },
            ) {
                PeerHeader(words)
                peers.forEach { peer -> Peer(peer, words, units, number, lossFormat) }
            }
        }
        if (footnote != null) MonarchText(text = footnote, style = MonarchTextStyle.Caption, color = muted)
    }
}

/**
 * The words of one person's block.
 *
 * @property connections the name of the list.
 * @property roundTrip "Round trip".
 * @property loss "Loss".
 * @property notMeasured "Not measured yet".
 * @property noValue the shown dash.
 * @property namedValue "%1$s %2$s".
 * @property qualities the name of each grade.
 */
private class PeerWords(
    val connections: String,
    val roundTrip: String,
    val loss: String,
    val notMeasured: String,
    val noValue: String,
    val namedValue: String,
    val qualities: Map<MonarchCallStatsPanelQuality, String>,
)

/** The header row of the table of people. */
@Composable
private fun PeerHeader(words: PeerWords) {
    val tokens = MonarchCallStatsPanelTokens
    val muted = tokens.muted(MonarchTheme.colors)
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = tokens.InnerGap * 2)) {
            MonarchText(
                text = stringResource(R.string.monarch_callstatspanel_person),
                style = MonarchTextStyle.Caption,
                color = muted,
                modifier = Modifier.weight(tokens.NameWeight),
            )
            MonarchText(
                text = words.roundTrip,
                style = MonarchTextStyle.Caption,
                color = muted,
                modifier = Modifier.weight(tokens.ValueWeight),
            )
            MonarchText(
                text = words.loss,
                style = MonarchTextStyle.Caption,
                color = muted,
                modifier = Modifier.weight(tokens.ValueWeight),
            )
            MonarchText(
                text = stringResource(R.string.monarch_callstatspanel_link),
                style = MonarchTextStyle.Caption,
                color = muted,
                modifier = Modifier.weight(tokens.LinkWeight),
            )
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

/** One person as a table row: name, round trip, loss and the link bars (or "Not measured yet"), read as one element. */
@Composable
private fun Peer(
    peer: MonarchCallStatsPanelPeer,
    words: PeerWords,
    units: MonarchCallStatsPanelUnits,
    number: (Double, Int) -> String,
    lossFormat: (Double) -> String,
) {
    val tokens = MonarchCallStatsPanelTokens
    val roundTrip = MonarchCallStatsPanelFormat.roundTrip(peer.roundTripMilliseconds, units, number)
    val loss = MonarchCallStatsPanelFormat.loss(peer)?.let(lossFormat)
    val grade = words.qualities.getValue(peer.quality)
    val spoken = listOf(
        peer.name,
        words.namedValue.format(words.roundTrip, roundTrip ?: words.notMeasured),
        words.namedValue.format(words.loss, loss ?: words.notMeasured),
        grade,
    ).joinToString(", ")
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = tokens.InnerGap * 3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(
                text = peer.name,
                style = MonarchTextStyle.Caption,
                modifier = Modifier.weight(tokens.NameWeight),
            )
            MonarchText(
                text = roundTrip ?: words.noValue,
                style = MonarchTextStyle.Caption,
                modifier = Modifier.weight(tokens.ValueWeight),
            )
            MonarchText(
                text = loss ?: words.noValue,
                style = MonarchTextStyle.Caption,
                modifier = Modifier.weight(tokens.ValueWeight),
            )
            Box(modifier = Modifier.weight(tokens.LinkWeight)) {
                val bars = tokens.bars(peer.quality)
                if (bars == 0) {
                    MonarchText(
                        text = grade,
                        style = MonarchTextStyle.Caption,
                        color = tokens.muted(MonarchTheme.colors),
                    )
                } else {
                    LinkBars(
                        bars,
                        tokens.barColor(peer.quality, MonarchTheme.colors),
                        MonarchTheme.colors.borderDefault,
                    )
                }
            }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

/** Three ascending bars, [filled] of them in [color]. */
@Composable
private fun LinkBars(filled: Int, color: Color, off: Color) {
    val tokens = MonarchCallStatsPanelTokens
    Canvas(modifier = Modifier.size(width = tokens.BarsWidth, height = tokens.BarsHeight)) {
        val bar = tokens.BarWidth.toPx()
        val gap = tokens.BarGap.toPx()
        for (i in 0 until BarCount) {
            val h = size.height * (i + 1) / BarCount
            drawRoundRect(
                if (i < filled) color else off,
                topLeft = Offset(i * (bar + gap), size.height - h),
                size = Size(bar, h),
                cornerRadius = CornerRadius(tokens.BarWidth.toPx() / BarCornerDivisor),
            )
        }
    }
}

/** Numbers in the current locale with a fixed number of decimals. */
@Composable
private fun rememberNumber(): (Double, Int) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { numberWriter(locale) }
}

/** Writes a number with 0 or 1 decimals in [locale], halves rounded up as on the web. */
private fun numberWriter(locale: Locale): (Double, Int) -> String {
    val formats = (0..1).map { digits ->
        NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
            roundingMode = BigDecimal.ROUND_HALF_UP
        }
    }
    return { value, digits -> formats[digits.coerceIn(0, 1)].format(value) }
}

/** A packet loss in percent, with one decimal, in the locale's percent format: "3.4%". */
@Composable
private fun rememberLossFormat(): (Double) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        val percent = NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
            roundingMode = BigDecimal.ROUND_HALF_UP
        }
        val writer: (Double) -> String = { percent.format(it / Hundred) }
        writer
    }
}

@Composable
private fun callUnits(): MonarchCallStatsPanelUnits = MonarchCallStatsPanelUnits(
    bytes = listOf(
        stringResource(R.string.monarch_callstatspanel_bytes),
        stringResource(R.string.monarch_callstatspanel_kilobytes),
        stringResource(R.string.monarch_callstatspanel_megabytes),
        stringResource(R.string.monarch_callstatspanel_gigabytes),
        stringResource(R.string.monarch_callstatspanel_terabytes),
    ),
    rates = listOf(
        stringResource(R.string.monarch_callstatspanel_bps),
        stringResource(R.string.monarch_callstatspanel_kbps),
        stringResource(R.string.monarch_callstatspanel_mbps),
    ),
    milliseconds = stringResource(R.string.monarch_callstatspanel_milliseconds),
)

@Composable
private fun peerWords(labels: MonarchCallStatsPanelLabels): PeerWords {
    val notMeasured = monarchString(labels.notMeasured, R.string.monarch_callstatspanel_not_measured)
    val defaults = mapOf(
        MonarchCallStatsPanelQuality.Good to stringResource(R.string.monarch_callstatspanel_good),
        MonarchCallStatsPanelQuality.Fair to stringResource(R.string.monarch_callstatspanel_fair),
        MonarchCallStatsPanelQuality.Poor to stringResource(R.string.monarch_callstatspanel_poor),
        MonarchCallStatsPanelQuality.Unknown to notMeasured,
    )
    return PeerWords(
        connections = monarchString(labels.connections, R.string.monarch_callstatspanel_connections),
        roundTrip = monarchString(labels.roundTrip, R.string.monarch_callstatspanel_round_trip),
        loss = monarchString(labels.loss, R.string.monarch_callstatspanel_loss),
        notMeasured = notMeasured,
        noValue = stringResource(R.string.monarch_callstatspanel_no_value),
        namedValue = stringResource(R.string.monarch_callstatspanel_named_value),
        qualities = defaults + labels.qualityNames,
    )
}

private const val Hundred = 100.0
private const val BarCount = 3
private const val BarCornerDivisor = 4
