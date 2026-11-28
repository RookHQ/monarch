package dev.monarchui.components.identity.permissionmatrix

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPermissionMatrix`. Every `null` uses the built-in string
 * named after it in `strings_permissionmatrix.xml`.
 *
 * @property permissionHeader the heading of the permission column of the grid: "Permission".
 * @property rolePicker the TalkBack name of the role tabs: "Role".
 * @property cellLabel the name of a cell in the grid, from the permission and the role: "Invite members for Admin".
 * @property allowed the read-only state of a granted cell: "Allowed".
 * @property notAllowed the read-only state of a cell that is not granted: "Not allowed".
 * @property locked the reason of a locked cell when `lockReason` returns an empty string: "This can’t be changed".
 */
@Immutable
public class MonarchPermissionMatrixLabels(
    public val permissionHeader: String? = null,
    public val rolePicker: String? = null,
    public val cellLabel: ((permission: String, role: String) -> String)? = null,
    public val allowed: String? = null,
    public val notAllowed: String? = null,
    public val locked: String? = null,
)
