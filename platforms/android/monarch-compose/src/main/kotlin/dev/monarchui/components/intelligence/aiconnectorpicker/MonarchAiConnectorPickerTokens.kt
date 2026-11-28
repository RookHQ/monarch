package dev.monarchui.components.intelligence.aiconnectorpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSearchText
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The sizes, colours and pure rules of MonarchAiConnectorPicker. */
internal object MonarchAiConnectorPickerTokens {
    /** The gap between the parts of the picker. */
    val Gap = MonarchTokens.Space.space3.dp

    /** The gap inside a connector. */
    val RowGap = MonarchTokens.Space.space2.dp

    /** The indent of a connector's tools. */
    val ToolIndent = MonarchTokens.Space.space4.dp

    /** Whether a connector counts as on: turned on and connected. */
    fun isOn(connector: MonarchAiConnector): Boolean =
        connector.enabled && connector.status == MonarchAiConnectorStatus.Connected

    /** How many connectors are on. */
    fun onCount(connectors: List<MonarchAiConnector>): Int = connectors.count(::isOn)

    /** How many tools of the connectors that are on ask first. */
    fun askingCount(connectors: List<MonarchAiConnector>): Int =
        connectors.filter(::isOn).sumOf { c -> c.tools.count { it.permission == MonarchAiConnectorToolPermission.Ask } }

    /** The connectors whose name contains [query], ignoring case, accents and spaces around it. */
    fun filter(connectors: List<MonarchAiConnector>, query: String): List<MonarchAiConnector> {
        val needle = fold(query)
        if (needle.isEmpty()) return connectors
        return connectors.filter { fold(it.name).contains(needle) }
    }

    /** The text in lower case without accents and spaces around it, for matching. */
    fun fold(text: String): String = MonarchSearchText.normalize(text.trim())

    /** The tone of a tool's risk badge. */
    fun riskTone(risk: MonarchAiConnectorToolRisk): MonarchTone = when (risk) {
        MonarchAiConnectorToolRisk.Read -> MonarchTone.Neutral
        MonarchAiConnectorToolRisk.Write -> MonarchTone.Warning
        MonarchAiConnectorToolRisk.Destructive -> MonarchTone.Danger
    }

    /** The colour of a status line. */
    fun statusColor(status: MonarchAiConnectorStatus, colors: MonarchColors): Color = when (status) {
        MonarchAiConnectorStatus.Connected -> colors.successStrong
        MonarchAiConnectorStatus.NeedsSignIn -> colors.warningStrong
        MonarchAiConnectorStatus.Error -> colors.dangerStrong
        MonarchAiConnectorStatus.Unavailable -> colors.textMuted
    }

    /** The colour of descriptions, reasons and the summary. */
    fun muted(colors: MonarchColors): Color = colors.textMuted
}
