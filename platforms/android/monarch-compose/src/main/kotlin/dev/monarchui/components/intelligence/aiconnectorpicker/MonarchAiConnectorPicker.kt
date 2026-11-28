package dev.monarchui.components.intelligence.aiconnectorpicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconSize
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.forms.radio.MonarchRadio
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Where a person chooses which connected apps and data sources an assistant may use in this conversation, and
 * whether each tool runs, asks first or never runs.
 *
 * The picker is the content of a sheet: present it in a `MonarchBottomSheet` (or a pane beside the
 * conversation on a large screen). Each connector is a row with a `MonarchSwitch` that toggles as a whole, its
 * favicon, its status in words and colour, and Sign in, Reconnect or Manage when the app handles them. Its tools
 * open under a "4 tools" button, each with a risk `MonarchBadge` and a row of `MonarchRadio`s: Allow, Ask first
 * and Never. A `MonarchSearchBar` filters connectors by name; a `MonarchEmptyState` says when nothing
 * matches.
 *
 * The state is hoisted: the picker hands the whole new list to [onConnectorsChange]. It refuses to turn on a
 * connector that is not connected and, unless [allowsDestructiveAutoApprove], to set a tool that can delete data
 * to Allow; store and enforce the permissions in the app as well.
 *
 * @param connectors the connectors with their state.
 * @param onConnectorsChange called with the new list when a switch or a permission changes.
 * @param modifier the modifier of the picker.
 * @param title replaces the heading "Connectors".
 * @param allowsDestructiveAutoApprove lets a tool that can delete data be set to Allow.
 * @param searchable shows the search field.
 * @param onConnect signs in to a connector that needs it, or reconnects one with a problem, given its id. Shows
 * Sign in or Reconnect on those rows.
 * @param onManage opens a connector's settings, given its id. Shows Manage on every row.
 * @param onDone shows Done next to the heading, which calls this. Close the sheet in it.
 * @param labels replaces any of the built-in strings.
 * @param initiallyExpandedIds the connectors whose tools are open when the picker first shows, as the web's
 * `defaultExpandedIds`. Each connector toggles itself after that.
 */
@Composable
public fun MonarchAiConnectorPicker(
    connectors: List<MonarchAiConnector>,
    onConnectorsChange: (List<MonarchAiConnector>) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    allowsDestructiveAutoApprove: Boolean = false,
    searchable: Boolean = true,
    onConnect: ((String) -> Unit)? = null,
    onManage: ((String) -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    labels: MonarchAiConnectorPickerLabels = MonarchAiConnectorPickerLabels(),
    initiallyExpandedIds: Set<String> = emptySet(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = MonarchAiConnectorPickerTokens.filter(connectors, query)
    val picker = Picker(
        connectors, onConnectorsChange, allowsDestructiveAutoApprove, onConnect, onManage, labels, initiallyExpandedIds,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.Gap),
    ) {
        Header(title, connectors, onDone, labels)
        if (searchable && connectors.isNotEmpty()) {
            MonarchSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = monarchString(labels.searchPrompt, R.string.monarch_aiconnectorpicker_search_prompt),
            )
        }
        when {
            connectors.isEmpty() -> MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_aiconnectorpicker_empty),
                size = MonarchEmptyStateSize.Sm,
            )
            shown.isEmpty() -> MonarchEmptyState(
                title = labels.noResults?.invoke(query.trim())
                    ?: stringResource(R.string.monarch_aiconnectorpicker_no_results, query.trim()),
                kind = MonarchEmptyStateKind.Filtered,
                size = MonarchEmptyStateSize.Sm,
                onClearFilters = { query = "" },
                clearFiltersLabel = monarchString(labels.clearSearch, R.string.monarch_aiconnectorpicker_clear_search),
            )
            else -> shown.forEach { connector -> ConnectorRow(picker, connector) }
        }
        // Always present, so the number found is announced when the search changes.
        val results = if (query.isBlank()) null else resultsText(shown.size, labels)
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (results != null) contentDescription = results
            },
        )
    }
}

/** What every row needs. */
private class Picker(
    val connectors: List<MonarchAiConnector>,
    val onChange: (List<MonarchAiConnector>) -> Unit,
    val allowsDestructive: Boolean,
    val onConnect: ((String) -> Unit)?,
    val onManage: ((String) -> Unit)?,
    val labels: MonarchAiConnectorPickerLabels,
    val initiallyExpanded: Set<String>,
) {
    fun setEnabled(id: String, enabled: Boolean) =
        onChange(MonarchAiConnectorPickerRules.setEnabled(connectors, id, enabled))

    fun setPermission(connectorId: String, toolId: String, permission: MonarchAiConnectorToolPermission) {
        val next = MonarchAiConnectorPickerRules.setPermission(
            connectors,
            connectorId,
            toolId,
            permission,
            allowsDestructive,
        )
        onChange(next)
    }
}

@Composable
private fun Header(
    title: String?,
    connectors: List<MonarchAiConnector>,
    onDone: (() -> Unit)?,
    labels: MonarchAiConnectorPickerLabels,
) {
    val on = MonarchAiConnectorPickerTokens.onCount(connectors)
    val asking = MonarchAiConnectorPickerTokens.askingCount(connectors)
    val summary = labels.summary?.invoke(on, connectors.size, asking) ?: stringResource(
        R.string.monarch_aiconnectorpicker_summary,
        pluralStringResource(R.plurals.monarch_aiconnectorpicker_connectors_on, connectors.size, on, connectors.size),
        pluralStringResource(R.plurals.monarch_aiconnectorpicker_tools_ask, asking, asking),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            MonarchText(
                text = title ?: monarchString(labels.title, R.string.monarch_aiconnectorpicker_title),
                style = MonarchTextStyle.Heading,
                heading = true,
            )
            MonarchText(
                text = summary,
                style = MonarchTextStyle.Caption,
                color = MonarchAiConnectorPickerTokens.muted(MonarchTheme.colors),
            )
        }
        if (onDone != null) {
            MonarchButton(
                text = monarchString(labels.done, R.string.monarch_aiconnectorpicker_done),
                onClick = onDone,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectorRow(picker: Picker, connector: MonarchAiConnector) {
    val labels = picker.labels
    val colors = MonarchTheme.colors
    val connected = connector.status == MonarchAiConnectorStatus.Connected
    val statusText = statusText(connector, labels)
    val reason = if (connected) null else connector.reason ?: disabledReason(connector.status, labels)
    val switchName = labels.enable?.invoke(connector.name)
        ?: stringResource(R.string.monarch_aiconnectorpicker_enable, connector.name)
    var toolsOpen by rememberSaveable(connector.id) { mutableStateOf(connector.id in picker.initiallyExpanded) }
    // A card per connector; the open one is outlined in the AI accent, as the web draws it.
    val body = @Composable {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.RowGap)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchTheme.shapes.sm)
                .toggleable(
                    value = connector.enabled,
                    enabled = connected,
                    role = Role.Switch,
                    onValueChange = { picker.setEnabled(connector.id, it) },
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(switchName, statusText, reason).joinToString(", ")
                },
            horizontalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchFavicon(image = connector.image, name = connector.name, size = MonarchFaviconSize.Md)
            Column(modifier = Modifier.weight(1f)) {
                MonarchText(text = connector.name, style = MonarchTextStyle.BodyStrong)
                connector.description?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchAiConnectorPickerTokens.muted(colors),
                    )
                }
                ConnectorStatusLine(connector.status, statusText)
                reason?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchAiConnectorPickerTokens.muted(colors),
                    )
                }
            }
            MonarchSwitch(checked = connector.enabled, onCheckedChange = null, enabled = connected)
        }
        val actions = actions(picker, connector)
        if (actions.isNotEmpty() || connector.tools.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.RowGap)) {
                actions.forEach { (text, onClick) -> Action(text, connector.name, labels, onClick) }
                if (connector.tools.isNotEmpty()) ToolsButton(connector, toolsOpen, labels) { toolsOpen = !toolsOpen }
            }
        }
        if (toolsOpen) {
            Column(
                modifier = Modifier.padding(start = MonarchAiConnectorPickerTokens.ToolIndent),
                verticalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.RowGap),
            ) {
                connector.tools.forEach { tool -> ToolRow(picker, connector, tool) }
            }
        }
    }
    }
    if (toolsOpen) {
        MonarchCard(tone = MonarchTone.Ai, contentPadding = PaddingValues(MonarchTheme.spacing.space3)) { body() }
    } else {
        body()
    }
}

@Composable
private fun actions(picker: Picker, connector: MonarchAiConnector): List<Pair<String, () -> Unit>> {
    val labels = picker.labels
    val connect = picker.onConnect
    val manage = picker.onManage
    val signIn = monarchString(labels.signIn, R.string.monarch_aiconnectorpicker_sign_in)
    val reconnect = monarchString(labels.reconnect, R.string.monarch_aiconnectorpicker_reconnect)
    return listOfNotNull(
        when {
            connect == null -> null
            connector.status == MonarchAiConnectorStatus.NeedsSignIn -> signIn to { connect(connector.id) }
            connector.status == MonarchAiConnectorStatus.Error -> reconnect to { connect(connector.id) }
            else -> null
        },
        manage?.let { monarchString(labels.manage, R.string.monarch_aiconnectorpicker_manage) to { it(connector.id) } },
    )
}

@Composable
private fun Action(text: String, connector: String, labels: MonarchAiConnectorPickerLabels, onClick: () -> Unit) {
    val name = labels.actionLabel?.invoke(text, connector)
        ?: stringResource(R.string.monarch_aiconnectorpicker_action_label, text, connector)
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = name },
        variant = MonarchButtonVariant.Secondary,
        size = MonarchButtonSize.Sm,
    )
}

@Composable
private fun ToolsButton(
    connector: MonarchAiConnector,
    open: Boolean,
    labels: MonarchAiConnectorPickerLabels,
    onClick: () -> Unit,
) {
    val count = connector.tools.size
    val text = labels.tools?.invoke(count)
        ?: pluralStringResource(R.plurals.monarch_aiconnectorpicker_tools, count, count)
    val name = labels.actionLabel?.invoke(text, connector.name)
        ?: stringResource(R.string.monarch_aiconnectorpicker_action_label, text, connector.name)
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = name
            if (open) {
                collapse {
                    onClick()
                    true
                }
            } else {
                expand {
                    onClick()
                    true
                }
            }
        },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
    )
}

@Composable
private fun ToolRow(picker: Picker, connector: MonarchAiConnector, tool: MonarchAiConnectorTool) {
    val labels = picker.labels
    val colors = MonarchTheme.colors
    val locked = MonarchAiConnectorPickerRules.locked(tool, picker.allowsDestructive)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiConnectorPickerTokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MonarchText(text = tool.name)
                tool.description?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchAiConnectorPickerTokens.muted(colors),
                    )
                }
            }
            MonarchBadge(
                text = riskName(tool.risk, labels),
                tone = MonarchAiConnectorPickerTokens.riskTone(tool.risk),
                leadingIcon = when (tool.risk) {
                    MonarchAiConnectorToolRisk.Read -> MonarchDefaultIcons.Eye
                    MonarchAiConnectorToolRisk.Write -> MonarchDefaultIcons.Pencil
                    MonarchAiConnectorToolRisk.Destructive -> MonarchDefaultIcons.Warning
                },
            )
        }
        PermissionMenu(picker, connector, tool)
        if (locked) {
            MonarchText(
                text = monarchString(labels.destructiveLocked, R.string.monarch_aiconnectorpicker_destructive_locked),
                style = MonarchTextStyle.Caption,
                color = MonarchAiConnectorPickerTokens.muted(colors),
            )
        }
    }
}

@Composable
private fun PermissionMenu(picker: Picker, connector: MonarchAiConnector, tool: MonarchAiConnectorTool) {
    val labels = picker.labels
    val current = permissionName(tool.permission, labels)
    val name = labels.permissionLabel?.invoke(tool.name)
        ?: stringResource(R.string.monarch_aiconnectorpicker_permission_label, tool.name)
    val enabled = MonarchAiConnectorPickerTokens.isOn(connector)
    val allowed = MonarchAiConnectorPickerRules.choices(tool, picker.allowsDestructive)
    // Radios side by side in one row, as the web draws the permission of a tool; the group is read as one
    // question. They keep the product accent, as on the web.
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup().semantics {
            contentDescription = name
            stateDescription = current
        },
    ) {
        MonarchAiConnectorToolPermission.entries.forEach { choice ->
            MonarchRadio(
                selected = choice == tool.permission,
                onClick = { picker.setPermission(connector.id, tool.id, choice) },
                modifier = Modifier.weight(1f),
                enabled = enabled && choice in allowed,
                label = permissionName(choice, labels),
            )
        }
    }
}

@Composable
private fun statusText(connector: MonarchAiConnector, labels: MonarchAiConnectorPickerLabels): String {
    val account = connector.account
    if (connector.status == MonarchAiConnectorStatus.Connected && account != null) {
        return labels.account?.invoke(account) ?: stringResource(R.string.monarch_aiconnectorpicker_account, account)
    }
    return labels.status?.invoke(connector.status) ?: stringResource(
        when (connector.status) {
            MonarchAiConnectorStatus.Connected -> R.string.monarch_aiconnectorpicker_status_connected
            MonarchAiConnectorStatus.NeedsSignIn -> R.string.monarch_aiconnectorpicker_status_needs_sign_in
            MonarchAiConnectorStatus.Error -> R.string.monarch_aiconnectorpicker_status_error
            MonarchAiConnectorStatus.Unavailable -> R.string.monarch_aiconnectorpicker_status_unavailable
        },
    )
}

@Composable
private fun disabledReason(status: MonarchAiConnectorStatus, labels: MonarchAiConnectorPickerLabels): String? {
    labels.disabledReason?.let { return it(status) }
    return when (status) {
        MonarchAiConnectorStatus.Connected -> null
        MonarchAiConnectorStatus.NeedsSignIn -> stringResource(R.string.monarch_aiconnectorpicker_reason_needs_sign_in)
        MonarchAiConnectorStatus.Error -> stringResource(R.string.monarch_aiconnectorpicker_reason_error)
        MonarchAiConnectorStatus.Unavailable -> stringResource(R.string.monarch_aiconnectorpicker_reason_unavailable)
    }
}

@Composable
private fun permissionName(
    permission: MonarchAiConnectorToolPermission,
    labels: MonarchAiConnectorPickerLabels,
): String =
    labels.permission?.invoke(permission) ?: stringResource(
        when (permission) {
            MonarchAiConnectorToolPermission.Allow -> R.string.monarch_aiconnectorpicker_permission_allow
            MonarchAiConnectorToolPermission.Ask -> R.string.monarch_aiconnectorpicker_permission_ask
            MonarchAiConnectorToolPermission.Never -> R.string.monarch_aiconnectorpicker_permission_never
        },
    )

@Composable
private fun riskName(risk: MonarchAiConnectorToolRisk, labels: MonarchAiConnectorPickerLabels): String =
    labels.risk?.invoke(risk) ?: stringResource(
        when (risk) {
            MonarchAiConnectorToolRisk.Read -> R.string.monarch_aiconnectorpicker_risk_read
            MonarchAiConnectorToolRisk.Write -> R.string.monarch_aiconnectorpicker_risk_write
            MonarchAiConnectorToolRisk.Destructive -> R.string.monarch_aiconnectorpicker_risk_destructive
        },
    )

@Composable
private fun resultsText(count: Int, labels: MonarchAiConnectorPickerLabels): String =
    labels.results?.invoke(count) ?: if (count == 0) {
        stringResource(R.string.monarch_aiconnectorpicker_results_none)
    } else {
        pluralStringResource(R.plurals.monarch_aiconnectorpicker_results, count, count)
    }

/** The status of a connector in words and colour, after its mark: check, lock, error or offline, as the web. */
@Composable
private fun ConnectorStatusLine(status: MonarchAiConnectorStatus, text: String) {
    val color = MonarchAiConnectorPickerTokens.statusColor(status, MonarchTheme.colors)
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            when (status) {
                MonarchAiConnectorStatus.Connected -> MonarchDefaultIcons.CheckCircle()
                MonarchAiConnectorStatus.NeedsSignIn -> MonarchDefaultIcons.Lock()
                MonarchAiConnectorStatus.Error -> MonarchDefaultIcons.Error()
                MonarchAiConnectorStatus.Unavailable -> MonarchDefaultIcons.Offline()
            }
        }
        MonarchText(text = text, style = MonarchTextStyle.Caption, color = color)
    }
}
