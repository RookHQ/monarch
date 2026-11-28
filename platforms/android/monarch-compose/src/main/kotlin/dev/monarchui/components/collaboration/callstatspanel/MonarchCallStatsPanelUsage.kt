package dev.monarchui.components.collaboration.callstatspanel

import androidx.compose.runtime.Immutable

/**
 * The data a call has used so far and its current rates, from the app's WebRTC statistics.
 *
 * @property sentBytes the bytes sent. Negative counts as 0.
 * @property receivedBytes the bytes received. Negative counts as 0.
 * @property upBitsPerSecond the current upload rate in bits per second. `null`, negative or not finite is 0 bps.
 * @property downBitsPerSecond the current download rate in bits per second. `null`, negative or not finite is
 * 0 bps.
 */
@Immutable
public class MonarchCallStatsPanelUsage(
    public val sentBytes: Long,
    public val receivedBytes: Long,
    public val upBitsPerSecond: Double? = null,
    public val downBitsPerSecond: Double? = null,
)
