package com.rookhq.monarch.components.intelligence.aipermissionscope

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiPermissionScope`.
 * Every `null` uses the built-in string named after it in
 * `strings_aipermissionscope.xml`.
 *
 * @property label the TalkBack name of the list: "What the AI can access".
 * @property access the full text of a level on a read-only row: "No access", "Can read", "Can read and edit".
 * @property option the short name of a level in the editable control: "None", "Read", "Write".
 * @property control the TalkBack name of a row's control, from the app: "Access to Calendar".
 * @property manage the text of the manage link: "Manage permissions".
 * @property empty the text shown when there are no apps: "The AI has no access to any app."
 */
@Immutable
public class MonarchAiPermissionScopeLabels(
    public val label: String? = null,
    public val access: ((MonarchAiAccessLevel) -> String)? = null,
    public val option: ((MonarchAiAccessLevel) -> String)? = null,
    public val control: ((app: String) -> String)? = null,
    public val manage: String? = null,
    public val empty: String? = null,
)
