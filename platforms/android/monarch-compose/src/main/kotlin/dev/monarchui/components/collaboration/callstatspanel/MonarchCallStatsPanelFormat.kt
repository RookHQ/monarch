package dev.monarchui.components.collaboration.callstatspanel

import kotlin.math.roundToLong

/**
 * The unit patterns of the panel, resolved from resources.
 *
 * @property bytes "%1$s B", "%1$s KB", "%1$s MB", "%1$s GB", "%1$s TB": powers of 1,024.
 * @property rates "%1$s bps", "%1$s kbps", "%1$s Mbps": powers of 1,000.
 * @property milliseconds "%1$s ms".
 */
internal class MonarchCallStatsPanelUnits(
    val bytes: List<String>,
    val rates: List<String>,
    val milliseconds: String,
)

/** The pure number rules of MonarchCallStatsPanel, as on the web. [number] writes a number with given decimals. */
internal object MonarchCallStatsPanelFormat {
    private const val KIBI = 1024.0
    private const val KILO = 1000.0
    private const val MEGA = 1_000_000.0
    private const val TEN = 10.0
    private const val TEN_MEGA = 10_000_000.0

    /** Bytes as "512 B", "1.4 KB", "12 MB": one decimal under 10 of a unit above bytes. Negative counts as 0. */
    fun bytes(bytes: Long, units: MonarchCallStatsPanelUnits, number: (Double, Int) -> String): String {
        var value = bytes.coerceAtLeast(0).toDouble()
        var unit = 0
        while (value >= KIBI && unit < units.bytes.lastIndex) {
            value /= KIBI
            unit += 1
        }
        val digits = if (unit == 0 || value >= TEN) 0 else 1
        return units.bytes[unit].format(number(value, digits))
    }

    /** Bits per second as "800 bps", "64 kbps", "1.5 Mbps" (no decimal from 10 Mbps). Bad values count as 0. */
    fun rate(bitsPerSecond: Double?, units: MonarchCallStatsPanelUnits, number: (Double, Int) -> String): String {
        val value = bitsPerSecond?.takeIf { it.isFinite() && it > 0 } ?: 0.0
        return when {
            value >= MEGA -> units.rates[2].format(number(value / MEGA, if (value >= TEN_MEGA) 0 else 1))
            value >= KILO -> units.rates[1].format(number((value / KILO).roundToLong().toDouble(), 0))
            else -> units.rates[0].format(number(value.roundToLong().toDouble(), 0))
        }
    }

    /** The round trip as "186 ms", rounded to whole milliseconds, or `null` when not measured. */
    fun roundTrip(milliseconds: Double?, units: MonarchCallStatsPanelUnits, number: (Double, Int) -> String): String? =
        milliseconds?.takeIf { it.isFinite() }?.let {
            units.milliseconds.format(number(it.roundToLong().toDouble(), 0))
        }

    /** The worse of the two losses that were measured, or `null` when neither was. */
    fun loss(peer: MonarchCallStatsPanelPeer): Double? =
        listOfNotNull(peer.inboundLossPercent, peer.outboundLossPercent).filter { it.isFinite() }.maxOrNull()
}
