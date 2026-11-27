package com.rookhq.monarch.components.intelligence.aiconnectorpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * A connected app or data source the assistant may use, such as a drive or a calendar.
 *
 * @property id the stable id, passed to `onConnect` and `onManage`.
 * @property name the visible name, already localised.
 * @property status whether it can be used.
 * @property enabled whether the person turned it on for this conversation. It counts as on only while
 * [status] is [MonarchAiConnectorStatus.Connected].
 * @property description a line about what the connector reaches.
 * @property account the account it is connected as, such as an email address.
 * @property reason why it cannot be used, shown under the status. A built-in reason is shown when `null`.
 * @property tools the tools it gives the assistant.
 * @property image its icon, loaded by the app. `null` shows a letter tile.
 */
@Immutable
public data class MonarchAiConnector(
    public val id: String,
    public val name: String,
    public val status: MonarchAiConnectorStatus,
    public val enabled: Boolean,
    public val description: String? = null,
    public val account: String? = null,
    public val reason: String? = null,
    public val tools: List<MonarchAiConnectorTool> = emptyList(),
    public val image: Painter? = null,
)
