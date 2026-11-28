package dev.monarchui.components.intelligence.aiconnectorpicker

import androidx.compose.runtime.Immutable

/**
 * A tool a connector gives the assistant, such as "Send email".
 *
 * @property id the stable id.
 * @property name the visible name, already localised.
 * @property risk what the tool can do to the person's data.
 * @property permission whether the assistant may run it.
 * @property description a line about the tool.
 */
@Immutable
public data class MonarchAiConnectorTool(
    public val id: String,
    public val name: String,
    public val risk: MonarchAiConnectorToolRisk,
    public val permission: MonarchAiConnectorToolPermission,
    public val description: String? = null,
)
