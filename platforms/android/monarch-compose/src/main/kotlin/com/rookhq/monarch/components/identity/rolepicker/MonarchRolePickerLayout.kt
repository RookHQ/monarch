package com.rookhq.monarch.components.identity.rolepicker

/** How a `MonarchRolePicker` shows its roles. */
public enum class MonarchRolePickerLayout {
    /** Every role as a radio card with its description and capabilities. */
    Cards,

    /** A menu field, with the chosen role's description under it. For forms with little room. */
    Compact,
}
