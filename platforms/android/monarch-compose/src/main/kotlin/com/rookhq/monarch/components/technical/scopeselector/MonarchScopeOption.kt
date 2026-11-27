package com.rookhq.monarch.components.technical.scopeselector

import androidx.compose.runtime.Immutable

/**
 * One permission a person can grant.
 *
 * @property value the scope string sent to the server, such as `projects:read`.
 * @property label the visible label. The [value] when `null`.
 * @property description what the scope allows, shown under the label.
 * @property enabled whether it can be changed. Select all skips a disabled scope.
 */
@Immutable
public class MonarchScopeOption(
    public val value: String,
    public val label: String? = null,
    public val description: String? = null,
    public val enabled: Boolean = true,
)
