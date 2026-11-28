package dev.monarchui.components.forms.combobox

/** Where a `MonarchCombobox` lists its options. */
public enum class MonarchComboboxPresentation {
    /** A menu for up to eight options, a sheet for longer lists and for lists the app searches. */
    Automatic,

    /** An editable field that filters a menu under it, the Material 3 exposed dropdown. */
    Menu,

    /** A field that opens a bottom sheet with a search field and the list. */
    Sheet,
}
