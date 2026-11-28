package com.rookhq.monarch.components.collaboration.callstatspanel

import androidx.compose.runtime.Immutable

/**
 * The connection numbers of one remote person.
 *
 * @property id the stable id of the person.
 * @property name the person's name. Already localised.
 * @property roundTripMilliseconds the round trip time in milliseconds. `null` or not finite is not measured.
 * @property inboundLossPercent the packet loss of what you receive from them, in percent.
 * @property outboundLossPercent the packet loss they report on what you send, in percent.
 * @property quality the connection grade.
 */
@Immutable
public class MonarchCallStatsPanelPeer(
    public val id: String,
    public val name: String,
    public val roundTripMilliseconds: Double? = null,
    public val inboundLossPercent: Double? = null,
    public val outboundLossPercent: Double? = null,
    public val quality: MonarchCallStatsPanelQuality = MonarchCallStatsPanelQuality.Unknown,
)
