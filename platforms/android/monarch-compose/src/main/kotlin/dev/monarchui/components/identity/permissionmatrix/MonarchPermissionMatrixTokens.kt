package dev.monarchui.components.identity.permissionmatrix

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The sizes, colours and pure rules of MonarchPermissionMatrix. */
internal object MonarchPermissionMatrixTokens {
    /** The narrowest the permission column of the grid gets. */
    val PermissionColumnMinWidth = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp

    /** The width of a role column of the grid. */
    val CellWidth = MonarchTokens.Space.space20.dp

    /** The gap between sections. */
    val Gap = MonarchTokens.Space.space3.dp

    /** The padding of a grid row. */
    val RowPadding = MonarchTokens.Space.space1.dp

    /** The largest font scale at which the automatic layout still shows the grid. */
    const val GridMaxFontScale = 1.3f

    /** The visible mark of a granted cell when read only. */
    const val AllowedMark = "✓"

    /** The visible mark of a cell that is not granted when read only. */
    const val NotAllowedMark = "–"

    /** Whether the automatic layout shows the grid: the columns fit [width] and the text is not enlarged much. */
    fun fitsGrid(width: Dp, roleCount: Int, fontScale: Float): Boolean =
        fontScale <= GridMaxFontScale && width >= PermissionColumnMinWidth + CellWidth * roleCount

    /** The width of the permission column of the grid in [width]: the rest of the row, at least its minimum. */
    fun permissionColumnWidth(width: Dp, roleCount: Int): Dp =
        maxOf(PermissionColumnMinWidth, width - CellWidth * roleCount)

    /** The value with [permissionId] granted to or taken from [roleId]. Other roles are kept as they are. */
    fun toggled(
        value: Map<String, Set<String>>,
        roleId: String,
        permissionId: String,
        granted: Boolean,
    ): Map<String, Set<String>> {
        val current = value[roleId].orEmpty()
        val next = if (granted) current + permissionId else current - permissionId
        return value + (roleId to next)
    }

    /** The permissions grouped by heading, groups in the order they first appear. */
    fun groups(
        permissions: List<MonarchPermissionMatrixPermission>,
    ): List<Pair<String?, List<MonarchPermissionMatrixPermission>>> = permissions.groupBy { it.group }.toList()

    /** The reason a cell is locked, with the built-in reason for an empty string, or `null` when it can change. */
    fun lockReason(reason: String?, fallback: String): String? = when {
        reason == null -> null
        reason.isBlank() -> fallback
        else -> reason
    }

    /** The colour of the read-only mark. */
    fun mark(granted: Boolean, colors: MonarchColors): Color = if (granted) colors.successStrong else colors.textSubtle

    /** The colour of descriptions, group headings and lock reasons. */
    fun muted(colors: MonarchColors): Color = colors.textMuted
}
