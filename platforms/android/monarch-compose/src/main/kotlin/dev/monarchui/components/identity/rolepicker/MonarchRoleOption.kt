package dev.monarchui.components.identity.rolepicker

import androidx.compose.runtime.Immutable

/**
 * One role of a `MonarchRolePicker`, with what it can do in plain words.
 *
 * @property value the value passed to `onSelect` when the role is chosen.
 * @property title the name of the role, such as "Editor".
 * @property description one sentence on what the role can do.
 * @property capabilities further things the role can do, one short line each.
 * @property enabled whether the role can be chosen. A role that cannot stays visible with [disabledReason].
 * @property disabledReason why the role cannot be chosen, shown when [enabled] is `false`.
 */
@Immutable
public class MonarchRoleOption<out T>(
    public val value: T,
    public val title: String,
    public val description: String? = null,
    public val capabilities: List<String> = emptyList(),
    public val enabled: Boolean = true,
    public val disabledReason: String? = null,
)
