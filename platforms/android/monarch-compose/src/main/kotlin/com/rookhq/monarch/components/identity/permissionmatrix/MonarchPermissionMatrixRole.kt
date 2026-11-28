package com.rookhq.monarch.components.identity.permissionmatrix

import androidx.compose.runtime.Immutable

/**
 * A role of a `MonarchPermissionMatrix`: a column of the grid, a tab of the one-role-at-a-time layout.
 *
 * @property id the stable id, the key of the value.
 * @property label the visible name, already localised.
 * @property description a line about the role, shown under the tabs in the one-role-at-a-time layout.
 */
@Immutable
public class MonarchPermissionMatrixRole(
    public val id: String,
    public val label: String,
    public val description: String? = null,
)
