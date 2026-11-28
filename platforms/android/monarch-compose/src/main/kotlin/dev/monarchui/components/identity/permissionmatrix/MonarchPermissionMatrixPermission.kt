package dev.monarchui.components.identity.permissionmatrix

import androidx.compose.runtime.Immutable

/**
 * A permission of a `MonarchPermissionMatrix`: a row.
 *
 * @property id the stable id, the value stored per role.
 * @property label the visible name, already localised.
 * @property description a line under the name.
 * @property group the heading the permission is listed under. Permissions with the same group are listed together,
 * groups in the order they first appear. `null` lists the permission without a heading.
 */
@Immutable
public class MonarchPermissionMatrixPermission(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val group: String? = null,
)
