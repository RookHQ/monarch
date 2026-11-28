package dev.monarchui.components.identity.permissionmatrix

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Which permissions each role has: roles by permissions, edited with checkboxes or shown read only, grouped, with
 * cells that cannot change and say why.
 *
 * A grid of roles by permissions does not fit a phone, so the [Automatic][MonarchPermissionMatrixLayout.Automatic]
 * layout shows the grid only when every column fits the width and the text is not enlarged much; otherwise it
 * shows one role at a time, picked with a Material 3 `PrimaryScrollableTabRow`, with a `MonarchCheckboxRow` per
 * permission. In the grid each cell is a Material 3 `Checkbox` named "<permission> for <role>".
 *
 * A locked cell is disabled and shows a lock mark in the grid; its reason is written under the row in the one-role
 * layout, and is in the cell's spoken name. Read only, each cell shows a
 * check or a dash and is read as Allowed or Not allowed.
 *
 * @param roles the roles, in order: columns of the grid, tabs of the one-role layout.
 * @param permissions the permissions, in order, listed by group.
 * @param value the granted permission ids by role id. A missing role has none.
 * @param onValueChange called with the whole new value when a cell is toggled.
 * @param modifier the modifier of the matrix.
 * @param title a heading above the matrix.
 * @param layout the grid, one role at a time, or whichever fits.
 * @param enabled whether the cells and tabs can be used, for example `false` while saving.
 * @param readOnly shows a check or a dash with Allowed or Not allowed instead of checkboxes.
 * @param lockReason given a role id and a permission id, why the cell cannot change, or `null` when it can. An
 * empty string uses "This can’t be changed". A locked cell keeps whatever [value] says.
 * @param labels replaces any of the built-in strings.
 */
@Composable
public fun MonarchPermissionMatrix(
    roles: List<MonarchPermissionMatrixRole>,
    permissions: List<MonarchPermissionMatrixPermission>,
    value: Map<String, Set<String>>,
    onValueChange: (Map<String, Set<String>>) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    layout: MonarchPermissionMatrixLayout = MonarchPermissionMatrixLayout.Automatic,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    lockReason: ((roleId: String, permissionId: String) -> String?)? = null,
    labels: MonarchPermissionMatrixLabels = MonarchPermissionMatrixLabels(),
) {
    val fallback = monarchString(labels.locked, R.string.monarch_permissionmatrix_locked)
    val matrix = Matrix(roles, permissions, value, onValueChange, enabled, readOnly, labels) { role, permission ->
        MonarchPermissionMatrixTokens.lockReason(lockReason?.invoke(role, permission), fallback)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchPermissionMatrixTokens.Gap),
    ) {
        if (title != null) MonarchText(text = title, style = MonarchTextStyle.Heading, heading = true)
        if (roles.isEmpty()) return@Column
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val grid = when (layout) {
                MonarchPermissionMatrixLayout.Grid -> true
                MonarchPermissionMatrixLayout.ByRole -> false
                MonarchPermissionMatrixLayout.Automatic ->
                    MonarchPermissionMatrixTokens.fitsGrid(maxWidth, roles.size, fontScale)
            }
            if (grid) {
                Grid(matrix, MonarchPermissionMatrixTokens.permissionColumnWidth(maxWidth, roles.size))
            } else {
                ByRole(matrix)
            }
        }
    }
}

/** What both layouts need. */
private class Matrix(
    val roles: List<MonarchPermissionMatrixRole>,
    val permissions: List<MonarchPermissionMatrixPermission>,
    val value: Map<String, Set<String>>,
    val onValueChange: (Map<String, Set<String>>) -> Unit,
    val enabled: Boolean,
    val readOnly: Boolean,
    val labels: MonarchPermissionMatrixLabels,
    val lock: (String, String) -> String?,
) {
    fun granted(role: String, permission: String): Boolean = permission in value[role].orEmpty()

    fun set(role: String, permission: String, granted: Boolean) {
        if (!enabled || readOnly || lock(role, permission) != null) return
        onValueChange(MonarchPermissionMatrixTokens.toggled(value, role, permission, granted))
    }
}

@Composable
private fun Grid(matrix: Matrix, permissionWidth: Dp) {
    val colors = MonarchTheme.colors
    val cellWidth = MonarchPermissionMatrixTokens.CellWidth
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.Bottom) {
            MonarchText(
                text = monarchString(
                    matrix.labels.permissionHeader,
                    R.string.monarch_permissionmatrix_permission_header,
                ),
                modifier = Modifier.width(permissionWidth),
                style = MonarchTextStyle.Label,
                color = MonarchPermissionMatrixTokens.muted(colors),
                heading = true,
            )
            matrix.roles.forEach { role ->
                MonarchText(
                    text = role.label,
                    modifier = Modifier.width(cellWidth),
                    style = MonarchTextStyle.Label,
                    textAlign = TextAlign.Center,
                    heading = true,
                )
            }
        }
        MonarchDivider(modifier = Modifier.width(permissionWidth + cellWidth * matrix.roles.size))
        MonarchPermissionMatrixTokens.groups(matrix.permissions).forEach { (group, permissions) ->
            if (group != null) GroupHeading(group, Modifier.padding(top = MonarchPermissionMatrixTokens.Gap))
            permissions.forEach { permission -> GridRow(matrix, permission, permissionWidth) }
        }
    }
}

@Composable
private fun GridRow(matrix: Matrix, permission: MonarchPermissionMatrixPermission, permissionWidth: Dp) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.padding(vertical = MonarchPermissionMatrixTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(permissionWidth)) {
            MonarchText(text = permission.label)
            permission.description?.let {
                MonarchText(
                    text = it,
                    style = MonarchTextStyle.Caption,
                    color = MonarchPermissionMatrixTokens.muted(colors),
                )
            }
        }
        matrix.roles.forEach { role ->
            Box(
                modifier = Modifier
                    .width(MonarchPermissionMatrixTokens.CellWidth)
                    .heightIn(min = MonarchTouchTarget.Minimum),
                contentAlignment = Alignment.Center,
            ) {
                GridCell(matrix, role, permission)
            }
        }
    }
}

@Composable
private fun GridCell(matrix: Matrix, role: MonarchPermissionMatrixRole, permission: MonarchPermissionMatrixPermission) {
    val name = matrix.labels.cellLabel?.invoke(permission.label, role.label)
        ?: stringResource(R.string.monarch_permissionmatrix_cell_label, permission.label, role.label)
    val granted = matrix.granted(role.id, permission.id)
    if (matrix.readOnly) {
        ReadOnlyMark(granted, name, matrix.labels)
        return
    }
    val reason = matrix.lock(role.id, permission.id)
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonarchCheckbox(
            checked = granted,
            onCheckedChange = { matrix.set(role.id, permission.id, it) },
            modifier = Modifier.semantics { contentDescription = if (reason != null) "$name, $reason" else name },
            enabled = matrix.enabled && reason == null,
        )
        // As the web: a lock mark in the cell; the reason is in the spoken name.
        if (reason != null) Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Lock() }
    }
}

@Composable
private fun ReadOnlyMark(granted: Boolean, name: String, labels: MonarchPermissionMatrixLabels) {
    val state = if (granted) {
        monarchString(labels.allowed, R.string.monarch_permissionmatrix_allowed)
    } else {
        monarchString(labels.notAllowed, R.string.monarch_permissionmatrix_not_allowed)
    }
    MonarchText(
        text = if (granted) MonarchPermissionMatrixTokens.AllowedMark else MonarchPermissionMatrixTokens.NotAllowedMark,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = name
            stateDescription = state
        },
        style = MonarchTextStyle.BodyStrong,
        color = MonarchPermissionMatrixTokens.mark(granted, MonarchTheme.colors),
    )
}

@Composable
private fun ByRole(matrix: Matrix) {
    val colors = MonarchTheme.colors
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, matrix.roles.lastIndex)
    val role = matrix.roles[index]
    val pickerName = monarchString(matrix.labels.rolePicker, R.string.monarch_permissionmatrix_role_picker)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchPermissionMatrixTokens.Gap)) {
        MonarchTabs(
            selectedIndex = index,
            onSelectedIndexChange = { selected = it },
            modifier = Modifier.semantics { contentDescription = pickerName },
        ) {
            matrix.roles.forEach { item -> tab(text = item.label, enabled = matrix.enabled) }
        }
        role.description?.let {
            MonarchText(
                text = it,
                style = MonarchTextStyle.Caption,
                color = MonarchPermissionMatrixTokens.muted(colors),
            )
        }
        MonarchPermissionMatrixTokens.groups(matrix.permissions).forEach { (group, permissions) ->
            Column {
                if (group != null) GroupHeading(group)
                permissions.forEach { permission -> RoleRow(matrix, role, permission) }
            }
        }
    }
}

@Composable
private fun RoleRow(
    matrix: Matrix,
    role: MonarchPermissionMatrixRole,
    permission: MonarchPermissionMatrixPermission,
) {
    val granted = matrix.granted(role.id, permission.id)
    val reason = matrix.lock(role.id, permission.id)
    val description = listOfNotNull(permission.description, reason).joinToString("\n").ifEmpty { null }
    if (matrix.readOnly) {
        val state = if (granted) {
            monarchString(matrix.labels.allowed, R.string.monarch_permissionmatrix_allowed)
        } else {
            monarchString(matrix.labels.notAllowed, R.string.monarch_permissionmatrix_not_allowed)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MonarchTouchTarget.Minimum)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(MonarchPermissionMatrixTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MonarchText(text = permission.label)
                description?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchPermissionMatrixTokens.muted(MonarchTheme.colors),
                    )
                }
            }
            MonarchText(text = state, style = MonarchTextStyle.Caption)
        }
        return
    }
    MonarchCheckboxRow(
        checked = granted,
        onCheckedChange = { matrix.set(role.id, permission.id, it) },
        label = permission.label,
        enabled = matrix.enabled && reason == null,
        description = description,
    )
}

@Composable
private fun GroupHeading(text: String, modifier: Modifier = Modifier) {
    MonarchText(
        text = text,
        modifier = modifier,
        style = MonarchTextStyle.Label,
        color = MonarchPermissionMatrixTokens.muted(MonarchTheme.colors),
        heading = true,
    )
}
