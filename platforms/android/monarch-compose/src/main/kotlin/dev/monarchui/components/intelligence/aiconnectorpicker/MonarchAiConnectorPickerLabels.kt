package dev.monarchui.components.intelligence.aiconnectorpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiConnectorPicker`. Every `null` uses the built-in string
 * named after it in `strings_aiconnectorpicker.xml`.
 *
 * @property title the heading: "Connectors". The `title` parameter wins.
 * @property searchPrompt the placeholder of the search field: "Search connectors".
 * @property done the button that calls `onDone`: "Done".
 * @property summary the line under the heading, from the connectors on, all connectors and the tools that ask
 * first: "2 of 4 connectors on · 3 tools ask first".
 * @property results announced when the search changes, from the number found: "2 connectors found".
 * @property noResults the empty state of a search, from the search text: "No connectors match “x”.".
 * @property clearSearch the button of that empty state: "Clear search".
 * @property empty the empty state without connectors: "No connectors are available.".
 * @property status the name of a status: "Connected", "Sign-in needed", "Connection problem", "Unavailable".
 * @property account the status of a connected connector with its account: "Connected as alex@example.com".
 * @property enable the spoken name of a connector's switch, from its name: "Use Drive in this conversation".
 * @property disabledReason why a connector cannot be turned on, from its status; `null` for none.
 * @property signIn the action of a connector that needs sign-in: "Sign in".
 * @property reconnect the action of a connector with a problem: "Reconnect".
 * @property manage the action that calls `onManage`: "Manage".
 * @property actionLabel the spoken name of an action, from the action and the connector: "Sign in: Drive".
 * @property tools the button that shows the tools, from their number: "4 tools".
 * @property permissionLabel the spoken name of a tool's permission menu, from the tool: "Permission for Send email".
 * @property permission the name of a permission: "Allow", "Ask first", "Never".
 * @property risk the name of a risk: "Reads data", "Changes data", "Can delete data".
 * @property destructiveLocked the line under a tool that can delete data and cannot be allowed.
 */
@Immutable
public class MonarchAiConnectorPickerLabels(
    public val title: String? = null,
    public val searchPrompt: String? = null,
    public val done: String? = null,
    public val summary: ((on: Int, total: Int, asking: Int) -> String)? = null,
    public val results: ((count: Int) -> String)? = null,
    public val noResults: ((query: String) -> String)? = null,
    public val clearSearch: String? = null,
    public val empty: String? = null,
    public val status: ((MonarchAiConnectorStatus) -> String)? = null,
    public val account: ((account: String) -> String)? = null,
    public val enable: ((name: String) -> String)? = null,
    public val disabledReason: ((MonarchAiConnectorStatus) -> String?)? = null,
    public val signIn: String? = null,
    public val reconnect: String? = null,
    public val manage: String? = null,
    public val actionLabel: ((action: String, connector: String) -> String)? = null,
    public val tools: ((count: Int) -> String)? = null,
    public val permissionLabel: ((tool: String) -> String)? = null,
    public val permission: ((MonarchAiConnectorToolPermission) -> String)? = null,
    public val risk: ((MonarchAiConnectorToolRisk) -> String)? = null,
    public val destructiveLocked: String? = null,
)
