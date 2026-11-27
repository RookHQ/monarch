package com.rookhq.monarch.components.technical.scopeselector

import androidx.compose.runtime.Immutable

/**
 * The scopes of one resource, such as one API.
 *
 * @property id a stable id, used for the expanded state and for auto-selection.
 * @property label the group's name, such as the API's name.
 * @property scopes the scopes the person may grant in this group, already filtered by the app.
 * @property description a line about the group, such as the API's owner.
 * @property loading whether the scopes are still loading.
 * @property emptyMessage shown instead of the count when the group has no scopes.
 */
@Immutable
public class MonarchScopeGroup(
    public val id: String,
    public val label: String,
    public val scopes: List<MonarchScopeOption>,
    public val description: String? = null,
    public val loading: Boolean = false,
    public val emptyMessage: String? = null,
)
