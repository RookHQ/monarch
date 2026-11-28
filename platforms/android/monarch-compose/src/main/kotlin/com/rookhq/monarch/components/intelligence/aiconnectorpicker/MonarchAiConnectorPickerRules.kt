package com.rookhq.monarch.components.intelligence.aiconnectorpicker

/** The rules MonarchAiConnectorPicker applies to the connectors it hands back. */
internal object MonarchAiConnectorPickerRules {
    /** The connectors with [id] turned on or off. Turning on a connector that is not connected is refused. */
    fun setEnabled(connectors: List<MonarchAiConnector>, id: String, enabled: Boolean): List<MonarchAiConnector> =
        connectors.map { c ->
            val refused = enabled && c.status != MonarchAiConnectorStatus.Connected
            if (c.id != id || refused) c else c.copy(enabled = enabled)
        }

    /** The permissions offered for [tool]: Allow is left out when it is locked, unless it is the stored value. */
    fun choices(tool: MonarchAiConnectorTool, allowsDestructive: Boolean): List<MonarchAiConnectorToolPermission> =
        MonarchAiConnectorToolPermission.entries.filter {
            it != MonarchAiConnectorToolPermission.Allow || !locked(tool, allowsDestructive) ||
                tool.permission == MonarchAiConnectorToolPermission.Allow
        }

    /** Whether Allow is refused for [tool]. */
    fun locked(tool: MonarchAiConnectorTool, allowsDestructive: Boolean): Boolean =
        tool.risk == MonarchAiConnectorToolRisk.Destructive && !allowsDestructive

    /** The connectors with a tool's permission changed. Allow on a locked tool is refused. */
    fun setPermission(
        connectors: List<MonarchAiConnector>,
        connectorId: String,
        toolId: String,
        permission: MonarchAiConnectorToolPermission,
        allowsDestructive: Boolean,
    ): List<MonarchAiConnector> = connectors.map { c ->
        if (c.id != connectorId) return@map c
        c.copy(
            tools = c.tools.map { tool ->
                val refused = permission == MonarchAiConnectorToolPermission.Allow && locked(tool, allowsDestructive)
                if (tool.id != toolId || refused) tool else tool.copy(permission = permission)
            },
        )
    }
}
