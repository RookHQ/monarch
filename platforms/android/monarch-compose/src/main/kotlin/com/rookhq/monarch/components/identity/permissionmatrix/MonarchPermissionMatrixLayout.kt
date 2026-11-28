package com.rookhq.monarch.components.identity.permissionmatrix

/** How a `MonarchPermissionMatrix` lays out its roles. The same names exist on every platform. */
public enum class MonarchPermissionMatrixLayout {
    /** The grid when it fits the width at the current font size, one role at a time otherwise. The default. */
    Automatic,

    /** Always the grid of roles by permissions, scrolling sideways when it is wider than the space. */
    Grid,

    /** Always one role at a time, picked with tabs. */
    ByRole,
}
