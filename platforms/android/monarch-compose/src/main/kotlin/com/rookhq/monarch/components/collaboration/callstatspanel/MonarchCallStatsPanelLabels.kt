package com.rookhq.monarch.components.collaboration.callstatspanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCallStatsPanel`. Every `null` uses the built-in string
 * named after it in `strings_callstatspanel.xml`.
 *
 * @property dataUsed the heading: "Data used this call".
 * @property usageSummary writes the usage line from the total, the upload rate and the download rate:
 * "15 MB total · up 420 kbps · down 1.3 Mbps".
 * @property lowData the note when low data mode is on: "Low data mode is on: audio only.".
 * @property connections the spoken name of the list of people: "Connection to each person".
 * @property roundTrip the name of the round trip time: "Round trip".
 * @property loss the name of the packet loss: "Loss".
 * @property noPeers the text when nobody else is connected: "No one else is connected yet.".
 * @property notMeasured how TalkBack reads a number that was not measured: "Not measured yet".
 * @property qualityNames the names of the grades. Missing grades keep "Good connection", "Unstable connection",
 * "Poor connection" and "Not measured yet".
 */
@Immutable
public class MonarchCallStatsPanelLabels(
    public val dataUsed: String? = null,
    public val usageSummary: ((total: String, up: String, down: String) -> String)? = null,
    public val lowData: String? = null,
    public val connections: String? = null,
    public val roundTrip: String? = null,
    public val loss: String? = null,
    public val noPeers: String? = null,
    public val notMeasured: String? = null,
    public val qualityNames: Map<MonarchCallStatsPanelQuality, String> = emptyMap(),
)
